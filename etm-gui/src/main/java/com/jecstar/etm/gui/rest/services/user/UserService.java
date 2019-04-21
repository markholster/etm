package com.jecstar.etm.gui.rest.services.user;

import com.jecstar.etm.gui.rest.AbstractGuiService;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalTagsJsonImpl;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.util.BCrypt;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Path("/user")
@DeclareRoles(SecurityRoles.ALL_ROLES)
@PermitAll
public class UserService extends AbstractGuiService {

    private static DataRepository dataRepository;
    private static EtmConfiguration etmConfiguration;
    private final String timezoneResponse;
    private final EtmPrincipalTags tags = new EtmPrincipalTagsJsonImpl();


    public static void initialize(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        UserService.dataRepository = dataRepository;
        UserService.etmConfiguration = etmConfiguration;
    }

    public UserService() {
        this.timezoneResponse = "{\"time_zones\": [" + Arrays.stream(TimeZone.getAvailableIDs()).map(tz -> escapeToJson(tz, true)).collect(Collectors.joining(",")) + "], " + escapeObjectToJsonNameValuePair("default_time_zone", TimeZone.getDefault().getID()) + "}";
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserSettings() {
        GetResponse getResponse = dataRepository.get(new GetRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId())
                .setFetchSource(null, new String[]{
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".searchtemplates",
                        ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + ".password_hash"}
                ));
        if (getResponse.isSourceEmpty()) {
            return "{}";
        }
        Map<String, Object> userObject = (Map<String, Object>) getResponse.getSourceAsMap().get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER);
        // Hack the max search history into the result. Dunno how to do this better.
        userObject.put("max_" + this.tags.getSearchHistorySizeTag(), etmConfiguration.getMaxSearchHistoryCount());
        return toString(userObject);
    }

    @PUT
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setUserSettings(String json) {
        Map<String, Object> valueMap = toMap(json);
        EtmPrincipal etmPrincipal = getEtmPrincipal();

        Map<String, Object> updateMap = new HashMap<>();
        Map<String, Object> userObject = new HashMap<>();

        if (!etmPrincipal.isLdapBase()) {
            userObject.put(this.tags.getNameTag(), valueMap.get(this.tags.getNameTag()));
            userObject.put(this.tags.getEmailTag(), valueMap.get(this.tags.getEmailTag()));
        }
        userObject.put(this.tags.getTimeZoneTag(), valueMap.get(this.tags.getTimeZoneTag()));
        userObject.put(this.tags.getLocaleTag(), valueMap.get(this.tags.getLocaleTag()));
        Integer newHistorySize = getInteger(this.tags.getSearchHistorySizeTag(), valueMap, EtmPrincipal.DEFAULT_HISTORY_SIZE);
        if (newHistorySize > etmConfiguration.getMaxSearchHistoryCount()) {
            newHistorySize = etmConfiguration.getMaxSearchHistoryCount();
        }
        userObject.put(this.tags.getSearchHistorySizeTag(), newHistorySize);
        userObject.put(this.tags.getDefaultSearchRangeTag(), valueMap.get(this.tags.getDefaultSearchRangeTag()));
        userObject.put(this.tags.getApiKeyTag(), valueMap.get(this.tags.getApiKeyTag()));

        updateMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, userObject);
        UpdateRequestBuilder builder = enhanceRequest(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + etmPrincipal.getId()),
                etmConfiguration
        )
                .setDoc(updateMap)
                .setDetectNoop(true);
        dataRepository.update(builder);

        if (newHistorySize < etmPrincipal.getHistorySize()) {
            // History size is smaller. Make sure the stored queries are sliced to the new size.
            Map<String, Object> scriptParams = new HashMap<>();
            scriptParams.put("history_size", newHistorySize);
            dataRepository.updateAsync(enhanceRequest(
                    new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId()),
                    etmConfiguration
                    )
                            .setScript(new Script(ScriptType.STORED, null, "etm_update-search-history", scriptParams)),
                    DataRepository.noopActionListener());
        }
        etmPrincipal.forceReload = true;
        return "{ \"status\": \"success\" }";
    }

    @GET
    @Path("/api_key")
    @Produces(MediaType.APPLICATION_JSON)
    public String createNewApiKey() {
        return "{" + this.escapeObjectToJsonNameValuePair("api_key", UUID.randomUUID().toString()) + "}";
    }

    @PUT
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setPassword(String json) {
        if (getEtmPrincipal().isLdapBase()) {
            // TODO gooi exceptie
            return null;
        }
        Map<String, Object> valueMap = toMap(json);

        String oldPassword = getString("current_password", valueMap);
        String newPassword = getString("new_password", valueMap);

        boolean valid = BCrypt.checkpw(oldPassword, getEtmPrincipal().getPasswordHash());
        if (!valid) {
            throw new EtmException(EtmException.INVALID_PASSWORD);
        }
        if (oldPassword.equals(newPassword)) {
            throw new EtmException(EtmException.PASSWORD_NOT_CHANGED);
        }
        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        Map<String, Object> updateMap = new HashMap<>();
        Map<String, Object> userObject = new HashMap<>();
        userObject.put(this.tags.getPasswordHashTag(), newHash);
        userObject.put(this.tags.getChangePasswordOnLogonTag(), false);

        updateMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, userObject);
        UpdateRequestBuilder builder = enhanceRequest(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.ETM_DEFAULT_TYPE, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER_ID_PREFIX + getEtmPrincipal().getId()),
                etmConfiguration
        )
                .setDoc(updateMap)
                .setDetectNoop(true);
        dataRepository.update(builder);
        return "{ \"status\": \"success\" }";
    }

    @GET
    @Path("/timezones")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTimeZones() {
        return this.timezoneResponse;
    }

    @GET
    @Path("/locales")
    @Produces(MediaType.APPLICATION_JSON)
    public String getLocales() {
        Locale requestedLocale = getEtmPrincipal().getLocale();
        return "{\"locales\": [" + Arrays.stream(Locale.getAvailableLocales()).filter(p -> p.getCountry().length() > 0).sorted(Comparator.comparing(o -> o.getDisplayName(requestedLocale))).map(l -> "{\"name\": " + escapeToJson(l.getDisplayName(requestedLocale), true) + ", \"value\": " + escapeToJson(l.toLanguageTag(), true) + "}")
                .collect(Collectors.joining(","))
                + "], \"default_locale\": {" + escapeObjectToJsonNameValuePair("name", Locale.getDefault().getDisplayName(requestedLocale))
                + ", " + escapeObjectToJsonNameValuePair("value", Locale.getDefault().toLanguageTag()) + "}}";
    }

    @GET
    @Path("/etminfo")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEvents() {
        StringBuilder result = new StringBuilder();
        NumberFormat numberFormat = NumberFormat.getInstance(getEtmPrincipal().getLocale());
        SearchRequestBuilder searchRequestBuilder = enhanceRequest(new SearchRequestBuilder().setIndices(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL), etmConfiguration)
                .setSize(0).setQuery(QueryBuilders.matchAllQuery());
        SearchResponse searchResponse = dataRepository.search(searchRequestBuilder);
        result.append("{");
        addLongElementToJsonBuffer("event_count", searchResponse.getHits().getTotalHits().value, result, true);
        addStringElementToJsonBuffer("event_count_as_string", numberFormat.format(searchResponse.getHits().getTotalHits()), result, false);
        addStringElementToJsonBuffer("etm_version", System.getProperty("app.version"), result, false);
        result.append("}");
        return result.toString();
    }
}