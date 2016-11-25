function buildUserPage() {
	var defaultLocale;
	var defaultTimeZone;
	$groupSelect = $('<select>').addClass('form-control custom-select etm-group');
	
	$.when(
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
	            }).autocompleteFieldQuery({queryKeywords: data.keywords, keywordGroupSelector: 'all'});       
	        }
	    }),
	    $.ajax({
	        type: 'GET',
	        contentType: 'application/json',
	        url: '../rest/user/timezones',
	        success: function(data) {
	            if (!data || !data.time_zones) {
	                return;
	            }
	            var $timezones = $('#sel-time-zone');
	            $(data.time_zones).each(function (index, timeZone) {
	                $timezones.append($('<option>').attr('value', timeZone).text(timeZone))
	            }); 
	            defaultTimeZone = data.default_time_zone;
	            $timezones.val(defaultTimeZone);
	        }
	    }),
	    $.ajax({
	        type: 'GET',
	        contentType: 'application/json',
	        url: '../rest/user/locales',
	        success: function(data) {
	            if (!data || !data.locales) {
	                return;
	            }
	            var $locales = $('#sel-locale');
	            $(data.locales).each(function (index, locale) {
	                $locales.append($('<option>').attr('value', locale.value).text(locale.name))
	            });
	            defaultLocale = data.default_locale.value;
	            $locales.val(defaultLocale);
	        }
	    }),
	    $.ajax({
	        type: 'GET',
	        contentType: 'application/json',
	        url: '../rest/settings/userroles',
	        success: function(data) {
	            if (!data || !data.user_roles) {
	                return;
	            }
	            var $rolesContainer = $('#user-roles-container');
	            $(data.user_roles).each(function (index, user_role) {
	            	$rolesContainer.append(
	            			$('<label>').addClass('form-check-inline').append(
	            					$('<input>').attr('type', 'checkbox').attr('id', 'check-role-' + user_role.value).attr('name', 'check-user-roles').attr('value', user_role.value).addClass('form-check-input'),
	            					'&nbsp;',
	            					user_role.name
	            			)
	            	);
	            });
	            
	        }
	    }),
	    $.ajax({
	        type: 'GET',
	        contentType: 'application/json',
	        url: '../rest/settings/groups',
	        success: function(data) {
	            if (!data || !data.groups) {
	            	// No groups, remove the fieldset.
	            	$('#lnk-add-group').parent().remove();
	                return;
	            }
	            $.each(data.groups, function(index, group) {
	            	$groupSelect.append($('<option>').attr('value', group.name).text(group.name));
	            });
	            sortSelectOptions($groupSelect);
	        }
	    })	    
	).done(function () {
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/users',
		    success: function(data) {
		        if (!data) {
		            return;
		        }
		        $userSelect = $('#sel-user');
		        $.each(data.users, function(index, user) {
		        	$userSelect.append($('<option>').attr('value', user.id).text(user.id + ' - ' + user.name));
		        	userMap[user.id] = user;
		        });
		        sortSelectOptions($userSelect)
		        $userSelect.val('');
		    }
		});
	});
	
	var userMap = {};
	$('#sel-user').change(function(event) {
		event.preventDefault();
		var userData = userMap[$(this).val()];
		if ('undefined' == typeof userData) {
			resetValues();
			return;
		}
		$('#list-groups').empty();
		$('#input-user-id').val(userData.id);
		$('#input-user-name').val(userData.name);
		$('#input-filter-query').val(userData.filter_query);
		$('#sel-filter-query-occurrence').val(userData.filter_query_occurrence);
		$('#sel-always-show-correlated-events').val(userData.always_show_correlated_events ? 'true' : 'false');
		$('#sel-locale').val(userData.locale);
		$('#sel-time-zone').val(userData.time_zone);
		$('#input-search-history-size').val(userData.search_history_size);
		$('#user-roles-container > label > input').prop('checked', false);
		if (userData.roles) {
			$.each(userData.roles, function(index, role) {
				$('#check-role-' + role).prop('checked', true);
			});
		}
		if (userData.groups) {
			$.each(userData.groups, function(index, groupName) {
				$('#list-groups').append(createGroupRow(groupName));
			});
		}
        $('#input-new-password1').val('');
        $('#input-new-password2').val('');		
		enableOrDisableButtons();
	});
	
	$('#btn-confirm-save-user').click(function(event) {
		$('#input-new-password1, #input-new-password2').parent().removeClass('has-danger');
		if (!document.getElementById('user_form').checkValidity()) {
			return false;
		}
		if (!checkOrInvalidateFormInCaseOfPasswordMismatch()) {
			return false;
		}
		event.preventDefault();
		var userId = $('#input-user-id').val();
		if (isUserExistent(userId)) {
			$('#overwrite-user-id').text(userId);
			$('#modal-user-overwrite').modal();
		} else {
			saveUser();
		}
	});
	
	$('#btn-save-user').click(function(event) {
		saveUser();
	});
	
	$('#btn-confirm-remove-user').click(function(event) {
		event.preventDefault();
		$('#remove-user-id').text($('#input-user-id').val());
        $('#modal-user-remove').modal();
	});	

	$('#btn-remove-user').click(function(event) {
		removeUser($('#input-user-id').val());
	});
	
	$('#lnk-add-group').click(function(event) {
		event.preventDefault();
		$('#list-groups').append(createGroupRow());
	});

	
	$('#input-user-id').on('input', enableOrDisableButtons);
		
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
		var userId = $('#input-user-id').val();
		if (userId) {
			$('#btn-confirm-save-user').removeAttr('disabled');
			if (isUserExistent(userId)) {
				$('#btn-confirm-remove-user').removeAttr('disabled');
				$('#input-new-password1, #input-new-password2').removeAttr('required');
			} else {
				$('#btn-confirm-remove-user').attr('disabled', 'disabled');
				$('#input-new-password1, #input-new-password2').attr('required', 'required');
			}
		} else {
			$('#btn-confirm-save-user, #btn-confirm-remove-user').attr('disabled', 'disabled');
		}
	}
	
    function createGroupRow(groupName) {
    	var groupRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
					$('<div>').addClass('input-group').append(
							$groupSelect.clone(true), 
							$('<span>').addClass('input-group-addon').append($('<a href="#">').addClass('fa fa-times text-danger').click(function (event) {event.preventDefault(); removeGroupRow($(this));}))
					)
			);
    	if (groupName) {
    		$(groupRow).find('.etm-group').val(groupName)
    	}
    	return groupRow;
    }

    function removeGroupRow(anchor) {
    	anchor.parent().parent().parent().remove();
    }
    
	function isUserExistent(userId) {
		return "undefined" != typeof userMap[userId];
	}
	
	function saveUser() {
		if (!checkOrInvalidateFormInCaseOfPasswordMismatch()) {
			return;
		}		
		var userData = createUserData();
		$.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/user/' + encodeURIComponent(userData.id),
            data: JSON.stringify(userData),
            success: function(data) {
                if (!data) {
                    return;
                }
        		if (!isUserExistent(userData.id)) {
        			$userSelect = $('#sel-user');
        			$userSelect.append($('<option>').attr('value', userData.id).text(userData.id + ' - ' + userData.name));
        			sortSelectOptions($userSelect);
        		}
        		userMap[userData.id] = userData;
        		$('#users_infoBox').text('User \'' + userData.id+ '\' saved.').show('fast').delay(5000).hide('fast');
            }
        }).always(function () {
        	$('#modal-user-overwrite').modal('hide');
        });  		
	}
	
	function removeUser(userId) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/user/' + encodeURIComponent(userId),
            success: function(data) {
                if (!data) {
                    return;
                }
        		delete userMap[userId];
        		$("#sel-user > option").filter(function(i){
        		       return $(this).attr("value") == userId;
        		}).remove();
        		$('#users_infoBox').text('User \'' + userId + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        }).always(function() {
        	$('#modal-user-remove').modal('hide');
        });  		
	}
	
	function checkOrInvalidateFormInCaseOfPasswordMismatch() {
        var new1 = $('#input-new-password1').val();
        var new2 = $('#input-new-password2').val();
        if (new1 !== new2) {
            $('#input-new-password1, #input-new-password2').parent().addClass('has-danger');
            $("#users_errorBox").text('The new password didn\'t match the retyped password').show('fast');
            return false;
        }
        return true;
	}
	
	function createUserData() {
		var userData = {
			id: $('#input-user-id').val(),
			name: $('#input-user-name').val() ? $('#input-user-name').val() : null,
			filter_query: $('#input-filter-query').val() ? $('#input-filter-query').val() : null,
			filter_query_occurrence: $('#sel-filter-query-occurrence').val(),
			always_show_correlated_events: $('#sel-always-show-correlated-events').val() == 'true' ? true : false,
			locale: $('#sel-locale').val(),
			time_zone: $('sel-time-zone').val(),
			search_history_size: $('#input-search-history-size').val() ? Number($('#input-search-history-size').val()) : 0,
			roles: [],
			groups: []
		}
        if ($('#input-new-password1').val()) {
        	userData.new_password = $('#input-new-password1').val();
        }
		$('#user-roles-container > label > input:checked').each(function () {
			userData.roles.push($(this).val());
		});
		$('.etm-group').each(function () {
			var groupName = $(this).val();
			if (-1 == userData.groups.indexOf(groupName)) {
				userData.groups.push(groupName);
			}
		});
		return userData;
	}

	function resetValues() {
		$('#input-user-id').val('');
		$('#input-user-name').val('');
		$('#input-filter-query').val('');
		$('#sel-filter-query-occurrence').val('MUST');
		$('#sel-always-show-correlated-event').val('false');
		$('#input-search-history-size').val(5);
		$('#sel-locale').val(defaultLocale);
		$('#sel-time-zone').val(defaultTimeZone);
		$('#user-roles-container > label > input').prop('checked', false);
        $('#input-new-password1').val('');
        $('#input-new-password2').val('');
        $('#list-groups').empty();
		enableOrDisableButtons();
	}
}