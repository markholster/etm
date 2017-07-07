function buildGroupPage() {
    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/search/keywords/etm_event_all',
        success: function(data) {
            if (!data || !data.keywords) {
                return;
            }
            $('#input-filter-query').bind('keydown', function( event ) {
                if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                    event.stopPropagation();
                }
            }).autocompleteFieldQuery({queryKeywords: data.keywords});       
        }
    });
    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/settings/userroles',
        success: function(data) {
            if (!data || !data.user_roles) {
                return;
            }
            var $rolesContainer = $('#group-roles-container');
            $(data.user_roles).each(function (index, user_role) {
            	$rolesContainer.append(
        			$('<label>').addClass('custom-control custom-checkbox').append(
    					$('<input>').attr('type', 'checkbox').attr('id', 'check-role-' + user_role.value).attr('name', 'check-user-roles').attr('value', user_role.value).addClass('custom-control-input'),
    					$('<span>').addClass('custom-control-indicator'),
    					$('<span>').addClass('custom-control-description').text(user_role.name)
        			)
            	);
            });
            
        }
    });
	
	var groupMap = {};
	$('#sel-group').change(function(event) {
		event.preventDefault();
		var groupData = groupMap[$(this).val()];
		if ('undefined' == typeof groupData) {
			resetValues();
			return;
		}
		$('#input-group-name').val(groupData.name);
		$('#input-filter-query').val(groupData.filter_query);
		$('#sel-filter-query-occurrence').val(groupData.filter_query_occurrence);
		$('#sel-always-show-correlated-events').val(groupData.always_show_correlated_events ? 'true' : 'false');
		$('#group-roles-container > label > input').prop('checked', false);
		if (groupData.roles) {
			$.each(groupData.roles, function(index, role) {
				$('#check-role-' + role).prop('checked', true);
			});
		}
		enableOrDisableButtons();
	});
	
	$('#btn-confirm-save-group').click(function(event) {
		event.preventDefault();
		if (!document.getElementById('group_form').checkValidity()) {
			return;
		}
		var groupName = $('#input-group-name').val();
		if (isGroupExistent(groupName)) {
			$('#overwrite-group-name').text(groupName);
			$('#modal-group-overwrite').modal();
		} else {
			saveGroup();
		}
	});
	
	$('#btn-save-group').click(function(event) {
		saveGroup();
	});
	
	$('#btn-confirm-remove-group').click(function(event) {
		event.preventDefault();
		$('#remove-group-name').text($('#input-group-name').val());
        $('#modal-group-remove').modal();
	});	

	$('#btn-remove-group').click(function(event) {
		removeGroup($('#input-group-name').val());
	});
	
	$('#btn-confirm-import-group').click(function(event) {
		event.preventDefault();
		$("#sel-import-group").empty();
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/groups/ldap',
		    success: function(data) {
		        if (!data) {
		            return;
		        }
		        $groupSelect = $('#sel-import-group');
		        $.each(data.groups, function(index, group) {
		        	$groupSelect.append($('<option>').attr('value', group.name).text(group.name));
		        });
		        sortSelectOptions($groupSelect)
		        $groupSelect.val('');
		        $('#modal-group-import').modal();
		    }
		});		
	});

	$('#btn-import-group').click(function(event) {
		event.preventDefault();
		var groupName = $("#sel-import-group").val();
		if (!groupName) {
			return false;
		}
		$.ajax({
		    type: 'PUT',
		    contentType: 'application/json',
		    url: '../rest/settings/groups/ldap/import/' + encodeURIComponent(groupName),
		    success: function(group) {
		        if (!group) {
		            return;
		        }
				// First the group if it is already present
				$('#sel-group > option').each(function () {
				    if(group.name == $(this).attr('value')) {
				        $(this).remove();
				    }
				});
				// Now add the updated group
		        $('#sel-group').append($('<option>').attr('value', group.name).text(group.name));
		        sortSelectOptions($('#sel-group'));
		        groupMap[group.name] = group;
		        $('#sel-group').val(group.name).trigger('change');
		    }
		}).always(function () {
        	if ($('#modal-group-import').is(':visible')) {
        		$('#modal-group-import').modal('hide');
        	}
        });
	});
	
	$('#input-group-name').on('input', enableOrDisableButtons);
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/groups',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        if (data.has_ldap) {
	        	$('#btn-confirm-import-group').show();
	        }
	        $groupSelect = $('#sel-group');
	        $.each(data.groups, function(index, group) {
	        	$groupSelect.append($('<option>').attr('value', group.name).text(group.name));
	        	groupMap[group.name] = group;
	        });
	        sortSelectOptions($groupSelect)
	        $groupSelect.val('');
	    }
	});
	
	function sortSelectOptions($select) {
		var options = $select.children('option');
		options.detach().sort(function(a,b) {
		    var at = $(a).text();
		    var bt = $(b).text();         
		    return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
		});
		options.appendTo($select);
	}
	
	function enableOrDisableButtons() {
		var groupName = $('#input-group-name').val();
		if (groupName) {
			$('#btn-confirm-save-group').removeAttr('disabled');
			if (isGroupExistent(groupName)) {
				$('#btn-confirm-remove-group').removeAttr('disabled');
			} else {
				$('#btn-confirm-remove-group').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-group, #btn-confirm-remove-group').attr('disabled', 'disabled');
		}
	}
	
	function isGroupExistent(groupName) {
		return "undefined" != typeof groupMap[groupName];
	}
	
	function saveGroup() {
		var groupData = createGroupData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/group/' + encodeURIComponent(groupData.name),
            data: JSON.stringify(groupData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isGroupExistent(groupData.name)) {
        			$groupSelect = $('#sel-group');
        			$groupSelect.append($('<option>').attr('value', groupData.name).text(groupData.name));
        			sortSelectOptions($groupSelect);
        		}
        		groupMap[groupData.name] = groupData;
        		$('#groups_infoBox').text('Group \'' + groupData.name+ '\' saved.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
        	if ($('#modal-group-overwrite').is(':visible')) {
        		$('#modal-group-overwrite').modal('hide');
        	}
        });		
	}
	
	function removeGroup(groupName) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/group/' + encodeURIComponent(groupName),
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete groupMap[groupName];
        		$("#sel-group > option").filter(function(i){
        		       return $(this).attr("value") == groupName;
        		}).remove();
        		$('#groups_infoBox').text('Group \'' + groupName + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
        	if ($('#modal-group-remove').is(':visible')) {
        		$('#modal-group-remove').modal('hide');
        	}
        });	
	}
	
	function createGroupData() {
		var groupData = {
			name: $('#input-group-name').val(),
			filter_query: $('#input-filter-query').val() ? $('#input-filter-query').val() : null,
			filter_query_occurrence: $('#sel-filter-query-occurrence').val(),		
			always_show_correlated_events: $('#sel-always-show-correlated-events').val() == 'true' ? true : false,
			roles: []
		}
		$('#group-roles-container > label > input:checked').each(function () {
			groupData.roles.push($(this).val());
		});
		return groupData;
	}

	function resetValues() {
		$('#input-group-name').val('');
		$('#input-filter-query').val('');
		$('#sel-filter-query-occurrence').val('MUST');
		$('#sel-always-show-correlated-event').val('false');
		$('#group-roles-container > label > input').prop('checked', false);
		enableOrDisableButtons();
	}
}