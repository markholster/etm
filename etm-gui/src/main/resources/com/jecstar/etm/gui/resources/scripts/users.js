function buildUserPage() {
	$groupSelect = $('<select>').addClass('form-control custom-select etm-group');
    $notifierSelect = $('<select>').addClass('form-control custom-select etm-notifier');

	$.when(
	    $.ajax({
	        type: 'GET',
	        contentType: 'application/json',
	        url: '../rest/search/keywords/etm_event_all',
	        cache: false,
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
	        cache: false,
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
	        cache: false,
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
	        url: '../rest/settings/groups',
	        cache: false,
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
        }),
        $.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: '../rest/settings/notifiers/basics',
            cache: false,
            success: function (data) {
                if (!data || !data.notifiers) {
                    // No groups, remove the fieldset.
                    $('#lnk-add-notifier').parent().remove();
                    return;
                }
                $.each(data.notifiers, function (index, notifier) {
                    $notifierSelect.append($('<option>').attr('value', notifier.name).text(notifier.name));
                });
                sortSelectOptions($notifierSelect);
            }
        })
	).done(function () {
		$.ajax({
		    type: 'GET',
		    contentType: 'application/json',
		    url: '../rest/settings/users',
		    cache: false,
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
            cache: false,
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
        $('#list-groups, #list-notifiers').empty();
		$('#input-user-id').val(userData.id);
		$('#input-user-name').val(userData.name);
		$('#input-user-email').val(userData.email);
		$('#input-filter-query').val(userData.filter_query);
		$('#sel-filter-query-occurrence').val(userData.filter_query_occurrence);
		$('#sel-always-show-correlated-events').val(userData.always_show_correlated_events ? 'true' : 'false');
		$('#sel-locale').val(userData.locale);
		$('#sel-time-zone').val(userData.time_zone);
		$('#input-search-history-size').val(userData.search_history_size);
		$('#input-default-search-range').val(userData.default_search_range ? userData.default_search_range / 1000 : null);
		$('#sel-change-password-on-logon').val(userData.change_password_on_logon ? 'true' : 'false');
		$('#user-roles-container > label > input').prop('checked', false);
		if (userData.roles) {
			$('#card-acl').find('select').val('none');
            $.each(userData.roles, function(index, role) {
                $('#card-acl').find("option[value='" + role + "']").parent().val(role);
            });
		}
        if (userData.dashboard_datasources) {
            $('#dashboard-datasource-block').find("input[type='checkbox']").prop('checked', false);
            $.each(userData.dashboard_datasources, function (index, ds) {
                $('#check-dashboard-datasource-' + ds).prop('checked', true);
            });
        }
        if (userData.signal_datasources) {
            $('#signal-datasource-block').find("input[type='checkbox']").prop('checked', false);
            $.each(userData.signal_datasources, function (index, ds) {
                $('#check-signal-datasource-' + ds).prop('checked', true);
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
				    cache: false,
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
        if (userData.notifiers) {
            $.each(userData.notifiers, function (index, notifierName) {
                $('#list-notifiers').append(createNotifierRow(notifierName));
            });
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

    $('#link-request-download').click(function(event) {
        event.preventDefault();
        $('#modal-download-users').modal();
    });

    $('#btn-download-users').click(function(event) {
        event.preventDefault();
        hideModals($('#modal-download-users'));
        var q = {
            fileType: $('#sel-download-type').val()
        };
        window.location.href = '../rest/settings/download/users?q=' + encodeURIComponent(JSON.stringify(q));
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
		    cache: false,
		    success: function(user) {
		        if (!user) {
		            return;
		        }
				// First remove the user when it is already present
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
		    }
		}).always(function () {
		    hideModals($('#modal-user-import'));
        });
	});
	
	$('#lnk-add-group').click(function(event) {
		event.preventDefault();
		$('#list-groups').append(createGroupRow());
	});

    $('#lnk-add-notifier').click(function (event) {
        event.preventDefault();
        $('#list-notifiers').append(createNotifierRow());
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
				$('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {
                        event.preventDefault();
                        removeGroupOrNotifierRow($(this));
                    })
                )
			)
		);
    	if (groupName) {
    		$(groupRow).find('.etm-group').val(groupName)
    	}
    	return groupRow;
    }

    function createNotifierRow(notifierName) {
        var notifierRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
            $('<div>').addClass('input-group').append(
                $notifierSelect.clone(true),
                $('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {
                        event.preventDefault();
                        removeGroupOrNotifierRow($(this));
                    })
                )
            )
        );
        if (notifierName) {
            $(notifierRow).find('.etm-notifier').val(notifierName)
        }
        return notifierRow;
    }

    function removeGroupOrNotifierRow(anchor) {
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
            cache: false,
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
            }
        }).always(function () {
            hideModals($('#modal-user-overwrite'));
        });
	}
	
	function removeUser(userId) {
		$.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/user/' + encodeURIComponent(userId),
            cache: false,
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
        }).always(function () {
            hideModals($('#modal-user-remove'));
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
			default_search_range: $('#input-default-search-range').val() ? Number($('#input-default-search-range').val()) * 1000 : null,
			change_password_on_logon: $('#sel-change-password-on-logon').val() == 'true' ? true : false, 
			roles: [],
            groups: [],
            dashboard_datasources: $('#dashboard-datasource-block')
                .find("input[type='checkbox']:checked")
                .map(function () {
                    return $(this).val();
                }).get(),
            signal_datasources: $('#signal-datasource-block')
                .find("input[type='checkbox']:checked")
                .map(function () {
                    return $(this).val();
                }).get(),
            notifiers: []
		}
        if ($('#input-new-password1').val()) {
        	userData.new_password = $('#input-new-password1').val();
        }
		$('#card-acl').find('select').each(function () {
            if ($(this).val() !== 'none') {
                userData.roles.push($(this).val());
            }
        });
		$('.etm-group').each(function () {
			var groupName = $(this).val();
			if (-1 == userData.groups.indexOf(groupName)) {
				userData.groups.push(groupName);
			}
		});
        $('.etm-notifier').each(function () {
            var notifierName = $(this).val();
            if (-1 == userData.notifiers.indexOf(notifierName)) {
                userData.notifiers.push(notifierName);
            }
        });
		return userData;
	}

	function resetValues() {
		document.getElementById('user_form').reset();
		enableOrDisableButtons();
	}
}