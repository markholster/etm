function buildMenu(currentContext) {
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/user/menu',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $.each(data.items, function(index, item) {
	        	addToMenu(currentContext, item.name, data.license_expired, data.license_almost_expired);
	        });
	        
	    }
	});
	
	function addToMenu(currentContext, menu, licenseExpired, licenseWarning) {
		var $li = $('<li>').addClass('nav-item');
		if (currentContext == menu)  {
			$li.addClass('active');
		}
		if ('search' == menu) {
			$li.append(createMenuLink('../search/', 'fa-search', 'Search'));
		} else if ('dashboard' == menu) {
// No dashboard yet			
//			$li.append(createMenuLink('../dashboard/', 'fa-dashboard', 'Dashboard'));
		} else if ('preferences' == menu) {
			$li.append(createMenuLink('../preferences/', 'fa-user', 'Preferences'));
		} else if ('settings' == menu) {
			var $license = $('<a>').addClass('dropdown-item').attr('href', '../settings/license.html');
			var licenseClass = '';
			if (licenseExpired) {
				$license.addClass('alert-danger').append(
					$('<span>').addClass('fa fa-ban hidden-sm-down').html('&nbsp;'), 
					'License'
				);
			} else if (licenseWarning) {
				$license.addClass('alert-warning').append(
					$('<span>').addClass('fa fa-exclamation-triangle hidden-sm-down').html('&nbsp;'), 
					'License'
				);
			} else {
				$license.text('License');
			}
			$li.addClass('dropdown').append(
					$('<a>').addClass('nav-link dropdown-toggle').attr('data-toggle', 'dropdown').attr('role', 'button').attr('aria-haspopup', true).attr('aria-expanded', 'false').attr('href', '#').append(
							$('<span>').addClass('fa fa-wrench fa-lg hidden-sm-down').html('&nbsp;'), 
							'Settings'
					),
					$('<div>').addClass('dropdown-menu').append(
							$('<a>').addClass('dropdown-item').attr('href', '../settings/endpoints.html').text('Endpoints'),
							$('<a>').addClass('dropdown-item').attr('href', '#').text('Nodes'),
							$('<a>').addClass('dropdown-item').attr('href', '../settings/parsers.html').text('Parsers'),
							$('<a>').addClass('dropdown-item').attr('href', '#').text('Users'),
							$('<div>').addClass('dropdown-divider'),
							$('<a>').addClass('dropdown-item').attr('href', '#').text('ES Nodes'),
							$('<a>').addClass('dropdown-item').attr('href', '#').text('ES Cluster'),
							$('<div>').addClass('dropdown-divider'),
							$license
					)
			);
		}
		$('#etm_mainmenu').append($li);
	}
	
	function createMenuLink(url, iconClass, text) {
		return $('<a>').addClass('nav-link').attr('href', url).append($('<span>').addClass('fa ' + iconClass + ' fa-lg hidden-sm-down').html('&nbsp;'), text);
	}
}