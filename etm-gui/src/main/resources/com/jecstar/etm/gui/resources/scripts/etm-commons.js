// Utility class for common ETM functions. This script should be loaded after jQuery.

// Method to retrieve query string parametetrs.
var queryString = (function(a) {
    if (a == "") return {};
    var b = {};
    for (var i = 0; i < a.length; ++i) {
        var p=a[i].split('=', 2);
        if (p.length == 1) {
            b[p[0]] = "";
        } else {
            b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
        }
    }
    return b;
})(window.location.search.substr(1).split('&'));

/**
* Determine if an element is in the viewport
**/
function isInViewport(elem) {
    	if (!$(elem).is(':visible')) {
    		return false;
    	}
        var docViewTop = $(window).scrollTop();
        var docViewBottom = docViewTop + $(window).height();
        var elemTop = $(elem).offset().top;
        var elemBottom = elemTop + $(elem).height();
        return docViewBottom > elemTop;
}

/**
* Add the ajax handlers that.
**/
function addAjaxHandlers(startFunction, stopFunction, errorFunction, completeFunction) {
    $(document).ajaxStart(function () {
        if (startFunction) {
            startFunction();
        }
        $('.errorBox').hide('fast');
        $('.navbar-brand').addClass('pulse');
    }).ajaxStop(function () {
        if (stopFunction) {
            stopFunction();
        }
        if (jQuery.active == 0) {
            $('.navbar-brand').removeClass('pulse');
        }
    }).ajaxError(errorFunction)
    .ajaxComplete(function(event, jqXHR, settings) {
        var contentType = jqXHR.getResponseHeader("Content-Type");
        if (jqXHR.status === 200 && contentType.toLowerCase().indexOf("text/html") >= 0) {
            // assume that our login has expired - reload our current page
            window.location.reload();
            return;
        }
        if (completeFunction) {
            completeFunction(event, jqXHR, settings);
        }
    });
}

/**
* Sort the options on a select element.
*/
function sortSelectOptions($select) {
    var options = $select.children('option');
    options.detach().sort(function(a,b) {
        var at = $(a).text();
        var bt = $(b).text();
        return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
    });
    options.appendTo($select);
}

/**
* Function that hides one or more bootstrap modals.
*/
function hideModals(modals) {
    $.each(modals, function(index, modal) {
        if ($(modal).is(':visible')) {
            $(modal).modal('hide');
        }
    });
}

$(document).ready( function () {
    // Hide all acl aware elements if this is a readonly page.
    if ('true' === queryString['readonly']) {
        $("[data-element-type='acl-aware']").hide();
    }
    // Handle submenus.
    $('.dropdown-menu a.dropdown-toggle').on('click', function ( e ) {
        var $el = $(this);
        var $parent = $(this).offsetParent(".dropdown-menu");
        if (!$(this).next().hasClass('show') ) {
            $(this).parents('.dropdown-menu').first().find('.show').removeClass("show");
        }
        var $subMenu = $(this).next(".dropdown-menu");
        $subMenu.toggleClass( 'show' );

        $(this).parent("li").toggleClass('show');

        $(this).parents('li.nav-item.dropdown.show').on('hidden.bs.dropdown', function (e) {
            $('.dropdown-menu .show').removeClass("show");
        });

        if (!$parent.parent().hasClass('navbar-nav')) {
            $el.next().css({"top": $el[0].offsetTop, "left": $parent.outerWidth() - 4});
        }
        return false;
    });
} );