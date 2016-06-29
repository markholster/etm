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
	        	addToMenu(currentContext, item.name)
	        });
	        
	    }
	});
	
	function addToMenu(currentContext, menu) {
		var $li = $('<li>').addClass('nav-item');
		if (currentContext == menu)  {
			$li.addClass('active');
		}
		if ('search' == menu) {
			$li.append(createMenuLink('../search/', 'fa-search', 'Search'));
		} else if ('dashboard' == menu) {
			$li.append(createMenuLink('../dashboard/', 'fa-dashboard', 'Dashboard'));
		} else if ('preferences' == menu) {
			$li.append(createMenuLink('../preferences/', 'fa-user', 'Preferences'));
		} else if ('settings' == menu) {
			$li.append(createMenuLink('../settings/', 'fa-wrench', 'Settings'));
		}
		$('#etm_mainmenu').append($li);
	}
	
	function createMenuLink(url, iconClass, text) {
		return $('<a>').addClass('nav-link').attr('href', url).append($('<span>').addClass('fa ' + iconClass + ' fa-lg hidden-sm-down').html('&nbsp;'), text);
	}
}