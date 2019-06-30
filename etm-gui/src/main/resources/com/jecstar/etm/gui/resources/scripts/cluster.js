function buildClusterPage() {
    'use strict';

    const certificateMap = {};
    const maxCertFileSize = 1024 * 64;
    let timeZone;
    let uploadedCerts = [];

    $('#btn-save-general').on('click', function (event) {
        if (!validateForm('form-general')) {
			return;
		}
		event.preventDefault();
		saveCluster('General');
	});

    $('#btn-save-elasticsearch').on('click', function (event) {
        if (!validateForm('form-elasticsearch')) {
			return;
		}
		event.preventDefault();
		saveCluster('Elasticsearch');
	});

    $('#btn-save-persisting').on('click', function (event) {
        if (!validateForm('form-persisting')) {
			return;
		}
		event.preventDefault();
		saveCluster('Persisting');
	});

    $('#btn-save-ldap').on('click', function (event) {
        if (!validateForm('form-ldap')) {
			return;
		}
		event.preventDefault();
		saveLdap();
	});

    $('#btn-confirm-remove-ldap').on('click', function (event) {
		event.preventDefault();
		$('#modal-ldap-remove').modal();
	});

    $('#btn-save-notifications').on('click', function (event) {
        if (!validateForm('form-notifications')) {
            return;
        }
        event.preventDefault();
        saveCluster('Notifications');
    });

    $('#btn-remove-ldap').on('click', function (event) {
		event.preventDefault();
		$.ajax({
		    type: 'DELETE',
		    contentType: 'application/json',
		    url: '../rest/settings/ldap',
		    cache: false,
		    success: function(data) {
		        if (!data) {
		            return;
		        }
				document.getElementById('form-ldap').reset();
				$('#btn-confirm-remove-ldap').attr('disabled', 'disabled');
				commons.showNotification('Ldap configuration removed.', 'success');
		    }
		}).always(function () {
            commons.hideModals($('#modal-ldap-remove'));
        });
	});

    $('#btn-save-certificate').on('click', function (event) {
        event.preventDefault();
        const certData = createCertificateData();
        const id = $('#sel-certificate').val();
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/certificate/' + encodeURIComponent(id),
            cache: false,
            data: JSON.stringify(certData),
            success: function (data) {
                if (!data) {
                    return;
                }
                certificateMap[id].trust_anchor = certData.trust_anchor;
                certificateMap[id].usage = certData.usage;
                commons.showNotification('Certificate \'' + certificateMap[id].dn + '\' saved.', 'success');
            }
        }).always(function () {
            commons.hideModals($('#modal-certificate-overwrite'));
        });
    });

    $('#btn-confirm-remove-certificate').on('click', function (event) {
        event.preventDefault();
        const id = $('#sel-certificate').val();
        $('#remove-certificate-id').text(certificateMap[id].dn);
        $('#modal-certificate-remove').modal();
    });

    $('#btn-remove-certificate').on('click', function (event) {
        event.preventDefault();
        const id = $('#sel-certificate').val();
        $.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/certificate/' + encodeURIComponent(id),
            cache: false,
            success: function (data) {
                if (!data) {
                    return;
                }
                const dn = certificateMap[id].dn;
                delete certificateMap[id];
                $("#sel-certificate > option").filter(function () {
                    return $(this).attr("value") === id;
                }).remove();
                commons.showNotification('Certificate \'' + dn + '\' removed.', 'success');
                $('#sel-certificate').trigger('change');
                enableOrDisableCertificateButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-certificate-remove'));
        });
    });

    $('#btn-confirm-import-certificate').on('click', function (event) {
        event.preventDefault();
        $('#list-import-certificate-chain').empty();
        document.getElementById('form-import-certificate').reset();
        $('#sel-import-certificate-method').trigger('change');
        $('#modal-import-certificate').modal();
    });

    $('#lnk-download-certificates').on('click', function (event) {
        event.preventDefault();
        const $list = $('#list-import-certificate-chain').empty();
        if (!validateForm('form-import-certificate')) {
            return;
        }
        const host = $("#input-import-certificate-host").val();
        const port = $("#input-import-certificate-port").val();
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/settings/certificate/download/' + encodeURIComponent(host) + '/' + encodeURIComponent(port),
            cache: false,
            success: function (data) {
                if (!data || !data.certificates) {
                    return;
                }
                $(data.certificates).each(function (index, certificate) {
                    $('<div>').addClass('form-group row')
                        .append($('<div>').addClass('col-sm-3').append(function () {
                            if ("undefined" != typeof certificateMap[certificate.serial]) {
                                return $('<span class="imported">').text("Imported")
                            } else {
                                return $('<select>').addClass("form-control form-control-sm custom-select custom-select-sm")
                                    .append($('<option>').attr('value', 'ignore').attr('selected', 'selected').text('Ignore'))
                                    .append($('<option>').attr('value', 'import').text('Import'))
                            }
                        }))
                        .append($('<div>').addClass('col col-form-label col-form-label-sm').text(certificate.dn))
                        .append($('<input>').attr('type', 'hidden').val(certificate.serial))
                        .appendTo($list)
                });
                $('<hr>').prependTo($list);
                $list.find('.imported').parent().addClass('col-form-label col-form-label-sm');
                $list.find('select').last().val('import');
            }
        });
    });

    $('#btn-import-certificate').on('click', function (event) {
        event.preventDefault();
        const certData = {
            serials: []
        };
        if ('connect' === $('#sel-import-certificate-method').val()) {
            certData.host = $("#input-import-certificate-host").val();
            certData.port = Number($("#input-import-certificate-port").val());
        } else {
            certData.certificates = uploadedCerts;
        }

        $('#list-import-certificate-chain').find('.row').each(function (index, certRow) {
            if ('import' === $(certRow).find('select').val()) {
                certData.serials.push($(certRow).find('input').val())
            }
        });
        if (certData.serials.length === 0) {
            commons.hideModals($('#modal-import-certificate'));
            return true;
        }

        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '../rest/settings/certificate/import',
            cache: false,
            data: JSON.stringify(certData),
            success: function (data) {
                if (!data) {
                    return;
                }
                const $certSelect = $('#sel-certificate');
                let lastAdded;
                $.each(data.certificates, function (index, certificate) {
                    $certSelect.append($('<option>').attr('value', certificate.serial).text(certificate.dn));
                    certificateMap[certificate.serial] = certificate;
                    lastAdded = certificate.serial;
                });
                commons.sortSelectOptions($certSelect);
                if (lastAdded) {
                    $certSelect.val(lastAdded).trigger('change');
                }
                commons.showNotification('Certificate(s) imported.', 'success');
                enableOrDisableCertificateButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-import-certificate'));
        });
    });

    $('#sel-certificate').on('change', function (event) {
        event.preventDefault();
        const certificateData = certificateMap[$(this).val()];
        if ('undefined' == typeof certificateData) {
            document.getElementById('form-certificate').reset();
            return;
        }
        setCertificateData(certificateData);
    });

    $('#sel-import-certificate-method').on('change', function (event) {
        event.preventDefault();
        uploadedCerts = [];
        $('#list-import-certificate-chain').empty();
        const selected = $(this).val();
        $('#import-cert-connect-group').toggle('connect' === selected);
        $('#import-cert-upload-group').toggle('upload' === selected);
    });

    $('#certificate-import-file').on('change', function (event) {
        event.preventDefault();
        uploadedCerts = [];
        const $list = $('#list-import-certificate-chain').empty();
        $('<hr>').prependTo($list);

        $.each($(this)[0].files, function (index, file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                const contents = e.target.result;
                const loadedData = {
                    certificate: contents
                };
                uploadedCerts.push(contents);
                $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    url: '../rest/settings/certificate/load/',
                    data: JSON.stringify(loadedData),
                    cache: false,
                    success: function (data) {
                        if (!data || !data.certificates) {
                            return;
                        }
                        $(data.certificates).each(function (index, certificate) {
                            $('<div>').addClass('form-group row')
                                .append($('<div>').addClass('col-sm-3').append(function () {
                                    if ("undefined" != typeof certificateMap[certificate.serial]) {
                                        return $('<span class="imported">').text("Imported")
                                    } else {
                                        return $('<select>').addClass("form-control form-control-sm custom-select custom-select-sm")
                                            .append($('<option>').attr('value', 'ignore').attr('selected', 'selected').text('Ignore'))
                                            .append($('<option>').attr('value', 'import').text('Import'))
                                    }
                                }))
                                .append($('<div>').addClass('col col-form-label col-form-label-sm').text(certificate.dn))
                                .append($('<input>').attr('type', 'hidden').val(certificate.serial))
                                .appendTo($list)
                        });
                        $('<hr>').prependTo($list);
                        $list.find('.imported').parent().addClass('col-form-label col-form-label-sm');
                        $list.find('select').last().val('import');
                    }
                });

            };
            if (file.size < maxCertFileSize) {
                reader.readAsText(file);
            }
        });
    });

	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/cluster',
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        setClusterData(data)
	    }
	});
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/ldap',
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
            setLdapData(data);
	        $('#btn-confirm-remove-ldap').removeAttr('disabled');
	    }
	});

    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/settings/certificates',
        cache: false,
        success: function (data) {
            if (!data) {
                return;
            }
            timeZone = data.time_zone;
            const $certSelect = $('#sel-certificate');
            $.each(data.certificates, function (index, certificate) {
                $certSelect.append($('<option>').attr('value', certificate.serial).text(certificate.dn));
                certificateMap[certificate.serial] = certificate;
            });
            commons.sortSelectOptions($certSelect);
            $certSelect.val($("#sel-certificate option:first").val());
            $certSelect.trigger('change');
            enableOrDisableCertificateButtons();
        }
    });
	
	function saveCluster(context) {
        const clusterData = createClusterData(context);
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/cluster',
            cache: false,
            data: JSON.stringify(clusterData),
            success: function(data) {
                if (!data) {
                    return;
                }
                commons.showNotification(context + ' configuration saved.', 'success');
            }
        });    		
	}
	
	function saveLdap() {
        const ldapData = createLdapData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/ldap',
            cache: false,
            data: JSON.stringify(ldapData),
            success: function(data) {
                if (!data) {
                    return;
                }
				commons.showNotification('Ldap configuration saved.', 'success');
        		$('#btn-confirm-remove-ldap').removeAttr('disabled');
            }
        });    		
	}

    function enableOrDisableCertificateButtons() {
        if (Object.keys(certificateMap).length) {
            $('#btn-confirm-remove-certificate').removeAttr('disabled');
            $('#btn-save-certificate').removeAttr('disabled');
        } else {
            $('#btn-confirm-remove-certificate').attr('disabled', 'disabled');
            $('#btn-save-certificate').attr('disabled', 'disabled');
        }
    }
	
	function setClusterData(data) {
		$("#input-session-timeout").val(data.session_timeout);
		$("#input-import-profile-cache-size").val(data.import_profile_cache_size);
		$("#input-shards-per-index").val(data.shards_per_index);
		$("#input-replicas-per-index").val(data.replicas_per_index);
		$("#input-max-event-indices").val(data.max_event_index_count);
		$("#input-max-metrics-indices").val(data.max_metrics_index_count);
		$("#input-max-audit-log-indices").val(data.max_audit_log_index_count);
		$("#input-wait-for-active-shards").val(data.wait_for_active_shards);
		$("#input-retries-on-conflict").val(data.retry_on_conflict_count);
		$("#input-query-timeout").val(data.query_timeout);
		$("#input-search-export-max-rows").val(data.max_search_result_download_rows);
		$("#input-search-max-templates").val(data.max_search_template_count);
		$("#input-search-max-history-size").val(data.max_search_history_count);
		$("#input-visualization-max-graph-count").val(data.max_graph_count);
		$("#input-visualization-max-dashboard-count").val(data.max_dashboard_count);
		$("#input-signal-max-signal-count").val(data.max_signal_count);
		$("#input-enhancing-handler-count").val(data.enhancing_handler_count);
		$("#input-persisting-handler-count").val(data.persisting_handler_count);
		$("#input-event-buffer-size").val(data.event_buffer_size);
		$("#sel-wait-strategy").val(data.wait_strategy);
		$("#input-persisting-bulk-count").val(data.persisting_bulk_count);
		$("#input-persisting-bulk-size").val(data.persisting_bulk_size);
		$("#input-persisting-bulk-time").val(data.persisting_bulk_time);
		$("#input-persisting-bulk-threads").val(data.persisting_bulk_threads);
	}
	
	function createClusterData(context) {
        const clusterData = {};
        if ('General' === context) {
			clusterData.session_timeout = Number($("#input-session-timeout").val());
            clusterData.endpoint_configuration_cache_size = Number($("#input-endpoint-configuration-cache-size").val());
			clusterData.max_search_result_download_rows = Number($("#input-search-export-max-rows").val());
			clusterData.max_search_template_count = Number($("#input-search-max-templates").val());
			clusterData.max_search_history_count = Number($("#input-search-max-history-size").val());
			clusterData.max_graph_count = Number($("#input-visualization-max-graph-count").val());
			clusterData.max_dashboard_count = Number($("#input-visualization-max-dashboard-count").val());
			clusterData.max_signal_count = Number($("#input-signal-max-signal-count").val());
        } else if ('Elasticsearch' === context) {
			clusterData.shards_per_index = Number($("#input-shards-per-index").val());
			clusterData.replicas_per_index = Number($("#input-replicas-per-index").val());
			clusterData.max_event_index_count = Number($("#input-max-event-indices").val());
			clusterData.max_metrics_index_count = Number($("#input-max-metrics-indices").val());
			clusterData.max_audit_log_index_count = Number($("#input-max-audit-log-indices").val());
			clusterData.wait_for_active_shards = Number($("#input-wait-for-active-shards").val());
			clusterData.retry_on_conflict_count = Number($("#input-retries-on-conflict").val());
			clusterData.query_timeout = Number($("#input-query-timeout").val());
        } else if ('Persisting' === context) {
			clusterData.enhancing_handler_count = Number($("#input-enhancing-handler-count").val());
			clusterData.persisting_handler_count = Number($("#input-persisting-handler-count").val());
			clusterData.event_buffer_size = Number($("#input-event-buffer-size").val());
			clusterData.wait_strategy = $("#sel-wait-strategy").val();
			clusterData.persisting_bulk_count = Number($("#input-persisting-bulk-count").val());
			clusterData.persisting_bulk_size = Number($("#input-persisting-bulk-size").val());
			clusterData.persisting_bulk_time = Number($("#input-persisting-bulk-time").val());
			clusterData.persisting_bulk_threads = Number($("#input-persisting-bulk-threads").val());
		}
		return clusterData;
	}
	
	function setLdapData(data) {
		$('#input-ldap-host').val(data.host);
		$('#input-ldap-port').val(data.port);
		$('#sel-ldap-connection-security').val(data.connection_security);
		$('#input-ldap-bind-dn').val(data.bind_dn);
		$('#input-ldap-bind-password').val(decode(data.bind_password));
		$('#input-ldap-connection-pool-min').val(data.min_pool_size);
		$('#input-ldap-connection-pool-max').val(data.max_pool_size);
		$('#input-ldap-connection-test-base-dn').val(data.connection_test_base_dn);
		$('#input-ldap-connection-test-search-filter').val(data.connection_test_search_filter);
		$('#input-ldap-user-base-dn').val(data.user_base_dn);
		$('#input-ldap-user-search-filter').val(data.user_search_filter);
		$('#sel-ldap-user-search-in-subtree').val(data.user_search_in_subtree ? 'true' : 'false');
		$('#input-ldap-user-id-attribute').val(data.user_identifier_attribute);
		$('#input-ldap-user-fullname-attribute').val(data.user_full_name_attribute);
		$('#input-ldap-user-email-attribute').val(data.user_email_attribute);
		$('#input-ldap-user-member-of-groups-attribute').val(data.user_member_of_groups_attribute);
		$('#input-ldap-user-groups-query-base-dn').val(data.user_groups_query_base_dn);
		$('#input-ldap-user-groups-query-filter').val(data.user_groups_query_filter);
		$('#input-ldap-group-base-dn').val(data.group_base_dn);
		$('#input-ldap-group-search-filter').val(data.group_search_filter);
	}
	
	function createLdapData() {
        const ldapData = {};
		ldapData.host = $('#input-ldap-host').val();
		ldapData.port = Number($('#input-ldap-port').val());
		ldapData.connection_security = $('#sel-ldap-connection-security').val() ? $('#sel-ldap-connection-security').val() : null;
		ldapData.bind_dn = $('#input-ldap-bind-dn').val();
		ldapData.bind_password = encode($('#input-ldap-bind-password').val());
		ldapData.min_pool_size = Number($('#input-ldap-connection-pool-min').val());
		ldapData.max_pool_size = Number($('#input-ldap-connection-pool-max').val());
		ldapData.connection_test_base_dn = $('#input-ldap-connection-test-base-dn').val();
		ldapData.connection_test_search_filter = $('#input-ldap-connection-test-search-filter').val();
		ldapData.user_base_dn = $('#input-ldap-user-base-dn').val();
		ldapData.user_search_filter = $('#input-ldap-user-search-filter').val();
        ldapData.user_search_in_subtree = $('#sel-ldap-user-search-in-subtree').val() === 'true';
		ldapData.user_identifier_attribute = $('#input-ldap-user-id-attribute').val();
		ldapData.user_full_name_attribute = $('#input-ldap-user-fullname-attribute').val();
		ldapData.user_email_attribute = $('#input-ldap-user-email-attribute').val();
		ldapData.user_member_of_groups_attribute = $('#input-ldap-user-member-of-groups-attribute').val();
		ldapData.user_groups_query_base_dn = $('#input-ldap-user-groups-query-base-dn').val();
		ldapData.user_groups_query_filter = $('#input-ldap-user-groups-query-filter').val();
		ldapData.group_base_dn = $('#input-ldap-group-base-dn').val();
		ldapData.group_search_filter = $('#input-ldap-group-search-filter').val();

		return ldapData;
	}

    function setCertificateData(data) {
        $('#input-certificate-distinguished-name').val(data.dn);
        let momentValue = moment(data.not_before, 'x', true);
        if (momentValue.isValid()) {
            $('#input-certificate-not-before').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ssZ'));
        } else {
            $('#input-certificate-not-before').val('');
        }
        momentValue = moment(data.not_after, 'x', true);
        if (momentValue.isValid()) {
            $('#input-certificate-not-after').val(momentValue.tz(timeZone).format('YYYY-MM-DDTHH:mm:ssZ'));
        } else {
            $('#input-certificate-not-after').val('');
        }
        $('#input-certificate-issued-by').val(data.issuer_dn ? data.issuer_dn : '');
        $('#sel-certificate-trust-anchor').val(data.self_signed ? 'true' : (data.trust_anchor ? 'true' : 'false')).prop('disabled', data.self_signed);
        $('[id^=check-type-]').prop('checked', false);
        $.each(data.usage, function (index, usage) {
            $('#check-type-' + usage.toLowerCase()).prop('checked', true);
        });
        $('#input-certificate-sha1').val(data.fingerprint_sha1.toUpperCase().match(/.{1,2}/g).join(":"));
    }

    function createCertificateData() {
        const certData = {
            trust_anchor: $('#sel-certificate-trust-anchor').val() === 'true',
            usage: $('[id^=check-type-]:checked').map(function () {
                return $(this).val();
            }).get()
        };
        return certData;
    }
	
	function decode(data) {
		if (!data) {
			return null;
		}
        for (let i = 0; i < 7; i++) {
            data = commons.base64Decode(data);
		}
		return data;
	}
	
	function encode(data) {
		if (!data) {
			return null;
		}
        for (let i = 0; i < 7; i++) {
            data = commons.base64Decode(data);
		}
		return data;
	}

    function validateForm(formId) {
        const $form = $('#' + formId);
        $form.find(':hidden').each(function () {
            $(this).find('[data-required="required"]').removeAttr('required');
        });

        let valid = false;
        if ($form[0].checkValidity()) {
            valid = true;
        }
        $form.find('[data-required]').attr('required', 'required');
        return valid;
    }

}