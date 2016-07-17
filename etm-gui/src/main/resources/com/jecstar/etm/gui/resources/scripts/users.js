function buildUserPage() {
	var defaultLocale;
	var defaultTimeZone;
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
    });
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
    });
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
    });
    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/settings/userroles',
        success: function(data) {
            if (!data || !data.user_roles) {
                return;
            }
            var $rolesContainer = $('#user-groups-container');
            $(data.user_roles).each(function (index, user_role) {
            	$rolesContainer.append(
            			$('<label>').addClass('checkbox-inline').append(
            					$('<input>').attr('type', 'checkbox').attr('id', 'check-role-' + user_role.value).attr('name', 'check-user-roles').attr('value', user_role.value),
            					user_role.name
            			)
            	);
            });
            
        }
    });
	
	var userMap = {};
	$('#sel-user').change(function(event) {
		event.preventDefault();
		var userData = userMap[$(this).val()];
		if ('undefined' == typeof userData) {
			resetValues();
			return;
		}
		$('#input-user-id').val(userData.id);
		$('#input-user-name').val(userData.name);
		$('#input-filter-query').val(userData.filter_query);
		$('#sel-locale').val(userData.locale);
		$('#sel-time-zone').val(userData.time_zone);
		$('#input-query-history-size').val(userData.query_history_size);
		$('#user-groups-container > label > input').prop('checked', false);
		if (userData.roles) {
			$.each(userData.roles, function(index, role) {
				$('#check-role-' + role).prop('checked', true);
			});
		}
        $('#input-new-password1').val('');
        $('#input-new-password2').val('');		
		enableOrDisableButtons();
	});
	
	$('#btn-confirm-save-user').click(function(event) {
		event.preventDefault();
		$('#input-new-password1, #input-new-password2').parent().removeClass('has-danger');
		if (!document.getElementById('user_form').checkValidity()) {
			return;
		}
		if (!checkOrInvalidateFormInCaseOfPasswordMismatch()) {
			return;
		}
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

	
	$('#input-user-id').on('input', enableOrDisableButtons);
	
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
			locale: $('#sel-locale').val(),
			time_zone: $('sel-time-zone').val(),
			query_history_size: $('#input-query-history-size').val() ? Number($('#input-query-history-size').val()) : 0,
			roles: []
		}
        if ($('#input-new-password1').val()) {
        	userData.new_password = $('#input-new-password1').val();
        }
		$('#user-groups-container > label > input:checked').each(function () {
			userData.roles.push($(this).val());
		});
		return userData;
	}

	function resetValues() {
		$('#input-user-id').val('');
		$('#input-user-name').val('')
		$('#input-filter-query').val('')
		$('#sel-locale').val(defaultLocale);
		$('#sel-time-zone').val(defaultTimeZone);
		$('#user-groups-container > label > input').prop('checked', false);
        $('#input-new-password1').val('');
        $('#input-new-password2').val('');
		enableOrDisableButtons();
	}
}