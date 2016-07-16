function buildUserPage() {
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
		enableOrDisableButtons();
	});
	
	$('#btn-confirm-save-user').click(function(event) {
		if (!document.getElementById('user_form').checkValidity()) {
			return;
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
			} else {
				$('#btn-confirm-remove-user').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-user, #btn-confirm-remove-user').attr('disabled', 'disabled');
		}
	}
	
	function isUserExistent(userId) {
		return "undefined" != typeof userMap[userId];
	}
	
	function saveUser() {
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
        		$('#modal-user-overwrite').modal('hide');
        		$('#users_infoBox').text('User \'' + userData.id+ '\' saved.').show('fast').delay(5000).hide('fast');
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
        		$('#modal-user-remove').modal('hide');
        		$('#users_infoBox').text('User \'' + userId + '\' removed.').show('fast').delay(5000).hide('fast');
            }
        });    		
	}
	
	function createUserData() {
		var userData = {
			id: $('#input-user-id').val(),
			name: $('#input-user-name').val() ? $('#input-user-name').val() : null,
			filter_query: $('#input-filter-query').val() ? $('#input-filter-query').val() : null
		}
		return userData;
	}

	function resetValues() {
		$('#input-user-id').val('');
		$('#input-user-name').val('')
		$('#input-filter-query').val('')
		enableOrDisableButtons();
	}
}