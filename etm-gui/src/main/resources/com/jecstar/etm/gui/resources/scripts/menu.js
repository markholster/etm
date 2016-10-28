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
	        	addToMenu(currentContext, item, data.license_expired, data.license_almost_expired);
	        });
	        
	    }
	});
	
	function addToMenu(currentContext, menu, licenseExpired, licenseWarning) {
		var $li = $('<li>').addClass('nav-item');
		if (currentContext == menu.name)  {
			$li.addClass('active');
		}
		if ('search' == menu.name) {
			$li.append(createMenuLink('../search/', 'fa-search', 'Search'));
		} else if ('dashboard' == menu.name) {
// Disable temporarely for the 2.0.0 release.			
//			$drowDown = $('<div>').addClass('dropdown-menu');
//			$.each(menu.dashboards, function(index, dashboard) {
//				$drowDown.append(
//						$('<a>').addClass('dropdown-item').attr('href', '../dashboard/index.html?name=' + encodeURIComponent(menu.name)).text(menu.name)
//				);
//			});
//			if (menu.dashboards) {
//				$drowDown.append($('<div>').addClass('dropdown-divider'));
//			}
//			$drowDown.append(
//					$('<a>').addClass('dropdown-item').attr('href', '../dashboard/index.html?new=true').text('New dashboard')
//			);
//			$li.addClass('dropdown').append(
//					$('<a>').addClass('nav-link dropdown-toggle').attr('data-toggle', 'dropdown').attr('role', 'button').attr('aria-haspopup', true).attr('aria-expanded', 'false').attr('href', '#').append(
//							$('<span>').addClass('fa fa-dashboard fa-lg hidden-sm-down').html('&nbsp;'), 
//							'Dashboards'
//					),
//					$drowDown
//			);			
		} else if ('preferences' == menu.name) {
			$li.append(createMenuLink('../preferences/', 'fa-user', 'Preferences'));
		} else if ('settings' == menu.name) {
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
			
			var $dropdown = $('<div>').addClass('dropdown-menu');
			if ($.inArray('admin', menu.submenus) != -1) {
				$dropdown.append(
						$('<a>').addClass('dropdown-item').attr('href', '../settings/users.html').text('Users'),
						$('<a>').addClass('dropdown-item').attr('href', '../settings/groups.html').text('Groups'),
						$('<div>').addClass('dropdown-divider'),
						$('<a>').addClass('dropdown-item').attr('href', '../settings/cluster.html').text('Cluster'),
						$('<a>').addClass('dropdown-item').attr('href', '../settings/parsers.html').text('Parsers'),
						$('<a>').addClass('dropdown-item').attr('href', '../settings/endpoints.html').text('Endpoints')
				);
			}
			if ($.inArray('iib_admin', menu.submenus) != -1) {
				if ($.inArray('admin', menu.submenus) != -1) {
					$dropdown.append($('<div>').addClass('dropdown-divider'));
				}
				$dropdown.append(
					$('<a>').addClass('dropdown-item').attr('href', '../iib/nodes.html').text('IIB Nodes'),
					$('<a>').addClass('dropdown-item').attr('href', '../iib/events.html').text('IIB Events')				
				);
			}
			if ($.inArray('admin', menu.submenus) != -1) {
				$dropdown.append(
					$('<div>').addClass('dropdown-divider'), 
					$license
				);
			}
			
			$li.addClass('dropdown').append(
					$('<a>').addClass('nav-link dropdown-toggle').attr('data-toggle', 'dropdown').attr('role', 'button').attr('aria-haspopup', true).attr('aria-expanded', 'false').attr('href', '#').append(
							$('<span>').addClass('fa fa-wrench fa-lg hidden-sm-down').html('&nbsp;'), 
							'Settings'
					),
					$dropdown
			);
			
			
		}
		$('#etm_mainmenu').append($li);
	}
	
	function createMenuLink(url, iconClass, text) {
		return $('<a>').addClass('nav-link').attr('href', url).append($('<span>').addClass('fa ' + iconClass + ' fa-lg hidden-sm-down').html('&nbsp;'), text);
	}
}