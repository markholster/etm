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
	            }).autocompleteFieldQuery({queryKeywords: data.keywords});       
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
	            	$option =  $('<option>').attr('value', timeZone).text(timeZone); 
	            	if (timeZone === data.default_time_zone) {
	            		$option.attr('selected', 'selected');
	            	}
	                $timezones.append($option)
	            }); 
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
	            	$option = $('<option>').attr('value', locale.value).text(locale.name);
	            	if (locale.value === data.default_locale.value) {
	            		$option.attr('selected', 'selected');
	            	}
	            	$locales.append($option)
	            });
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
            			$('<label>').addClass('custom-control custom-checkbox').append(
        					$('<input>').attr('type', 'checkbox').attr('id', 'check-role-' + user_role.value).attr('name', 'check-user-roles').attr('value', user_role.value).addClass('custom-control-input'),
        					$('<span>').addClass('custom-control-indicator'),
        					$('<span>').addClass('custom-control-description').text(user_role.name)
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
	            	if (!group.ldap_base) {
	            		$groupSelect.append($('<option>').attr('value', group.name).text(group.name));
	            	}
	            });
	            if ($groupSelect.children('option').length == 0) {
	            	// Only ldap groups present. remove the add-group link
	            	$('#lnk-add-group').remove();
	            }
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
		        if (data.has_ldap) {
		        	$('#btn-confirm-import-user').show();
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
	
    $("#input-import-user-id").autocomplete({
        source: function( request, response ) {
          $.ajax({
        	type: 'POST',
            url: "../rest/settings/users/ldap/search",
            contentType: 'application/json',
            data: JSON.stringify({ query: request.term }),
            success: function(data) {
            	if (!data) {
            		return
            	}
            	response(data);
            }
          });
        },
        minLength: 1,
      });
	
	
	var userMap = {};
	$('#sel-user').change(function(event) {
		event.preventDefault();
		var userData = userMap[$(this).val()];
		if ('undefined' == typeof userData) {
			resetValues();
			enableFieldsForNonLdapUser();
			return;
		}
		$('#list-groups').empty();
		$('#input-user-id').val(userData.id);
		$('#input-user-name').val(userData.name);
		$('#input-user-email').val(userData.email);
		$('#input-filter-query').val(userData.filter_query);
		$('#sel-filter-query-occurrence').val(userData.filter_query_occurrence);
		$('#sel-always-show-correlated-events').val(userData.always_show_correlated_events ? 'true' : 'false');
		$('#sel-locale').val(userData.locale);
		$('#sel-time-zone').val(userData.time_zone);
		$('#input-search-history-size').val(userData.search_history_size);
		$('#sel-change-password-on-logon').val(userData.change_password_on_logon ? 'true' : 'false');
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
			if (userData.ldap_base) {
				// load the ldap groups of the user
				$.ajax({
				    type: 'GET',
				    contentType: 'application/json',
				    url: '../rest/settings/user/' + encodeURIComponent(userData.id) + '/ldap/groups',
				    success: function(data) {
				        if (!data) {
				            return;
				        }
						$.each(data.groups, function(index, group) {
							$('#list-groups').append(createLdapGroupRow(group.name));
						});
				    }
				});				
			}
		}
        $('#input-new-password1').val('');
        $('#input-new-password2').val('');
        if (userData.ldap_base) {
        	disableFieldsForLdapUser();
        } else {
        	enableFieldsForNonLdapUser();
        }
		enableOrDisableButtons();
		
		function enableFieldsForNonLdapUser() {
        	$('#input-user-id').removeAttr('disabled');
        	$('#input-user-name').removeAttr('disabled');
        	$('#input-user-email').removeAttr('disabled');
        	$('#input-new-password1').removeAttr('disabled');
        	$('#input-new-password2').removeAttr('disabled');
        	$('#sel-change-password-on-logon').removeAttr('disabled');			
		}
		
		function disableFieldsForLdapUser() {
        	$('#input-user-id').attr('disabled', 'disabled');
        	$('#input-user-name').attr('disabled', 'disabled');
        	$('#input-user-email').attr('disabled', 'disabled');
        	$('#input-new-password1').attr('disabled', 'disabled');
        	$('#input-new-password2').attr('disabled', 'disabled');
        	$('#sel-change-password-on-logon').attr('disabled', 'disabled');			
		}
	});
	
	$('#btn-confirm-save-user').click(function(event) {
		$('#input-new-password1, #input-new-password2').parent().removeClass('has-danger');
		if (!document.getElementById('user_form').checkValidity()) {
			return;
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

	$('#btn-confirm-import-user').click(function(event) {
		event.preventDefault();
		$('#input-import-user-id').val('');
		$('#modal-user-import').modal();
	});
	
	$('#btn-import-user').click(function(event) {
		event.preventDefault();
		var userId = $("#input-import-user-id").val();
		if (!userId) {
			return false;
		}
		$.ajax({
		    type: 'PUT',
		    contentType: 'application/json',
		    url: '../rest/settings/users/ldap/import/' + encodeURIComponent(userId),
		    success: function(user) {
		        if (!user) {
		            return;
		        }
				// First the group if it is already present
				$('#sel-user > option').each(function () {
				    if(user.id == $(this).attr('value')) {
				        $(this).remove();
				    }
				});
				// Now add the updated user
		        $('#sel-user').append($('<option>').attr('value', user.id).text(user.id + ' - ' + user.name));
		        sortSelectOptions($('#sel-user'));
		        userMap[user.id] = user;
		        $('#sel-user').val(user.id).trigger('change');
		    },
		    complete: function() {
		    	$('#modal-user-import').modal('hide');
		    }
		});
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
	
    function createLdapGroupRow(groupName) {
    	return $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
    			$('<div>').addClass('input-group').append(groupName)
    	);
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
        			$userSelect.append($('<option>').attr('value', userData.id).text(userData.name ? (userData.id + ' - ' + userData.name) : (userData.id + ' - ' + userData.id)));
        			sortSelectOptions($userSelect);
        		}
        		userMap[userData.id] = userData;
        		$('#users_infoBox').text('User \'' + userData.id+ '\' saved.').show('fast').delay(5000).hide('fast');
            },
            complete: function () {
            	$('#modal-user-overwrite').modal('hide');            	
            }
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
            },
            complete: function() {
            	$('#modal-user-remove').modal('hide');
            }
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
			email: $('#input-user-email').val() ? $('#input-user-email').val() : null,
			filter_query: $('#input-filter-query').val() ? $('#input-filter-query').val() : null,
			filter_query_occurrence: $('#sel-filter-query-occurrence').val(),
			always_show_correlated_events: $('#sel-always-show-correlated-events').val() == 'true' ? true : false,
			locale: $('#sel-locale').val(),
			time_zone: $('sel-time-zone').val(),
			search_history_size: $('#input-search-history-size').val() ? Number($('#input-search-history-size').val()) : 0,
			change_password_on_logon: $('#sel-change-password-on-logon').val() == 'true' ? true : false, 
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
		document.getElementById('user_form').reset();
		enableOrDisableButtons();
	}
}