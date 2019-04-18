// Utility class for common ETM functions. This script should be loaded after jQuery.
const commons = (function () {
    "use strict";

    /**
     * Method to retrieve query string parameters.
     */
    const queryString = (function (a) {
        if (a === "") return {};
        const b = {};
        for (let i = 0; i < a.length; ++i) {
            const p = a[i].split('=', 2);
            if (p.length === 1) {
                b[p[0]] = "";
            } else {
                b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
            }
        }
        return b;
    })(window.location.search.substr(1).split('&'));

    /**
     * Determine if an element is in the viewport
     */
    const isInViewport = function isInViewport(elem) {
        if (!$(elem).is(':visible')) {
            return false;
        }
        const docViewTop = $(window).scrollTop();
        const docViewBottom = docViewTop + $(window).height();
        const elemTop = $(elem).offset().top;
        // const elemBottom = elemTop + $(elem).height();
        return docViewBottom > elemTop;
    };

    /**
     * Add the ajax handlers that.
     */
    const addAjaxHandlers = function addAjaxHandlers(startFunction, stopFunction, errorFunction, completeFunction) {
        $(document).ajaxStart(function () {
            if (startFunction) {
                startFunction();
            }
            $('.errorBox').hide('fast');
            $('.u-header-logo').addClass('pulse');
        }).ajaxStop(function () {
            if (stopFunction) {
                stopFunction();
            }
            if (jQuery.active === 0) {
                $('.u-header-logo').removeClass('pulse');
            }
        }).ajaxError(errorFunction)
            .ajaxComplete(function (event, jqXHR, settings) {
                const contentType = jqXHR.getResponseHeader("Content-Type");
                if (jqXHR.status === 200 && contentType.toLowerCase().indexOf("text/html") >= 0) {
                    // assume that our login has expired - reload our current page
                    window.location.reload();
                    return;
                }
                if (completeFunction) {
                    completeFunction(event, jqXHR, settings);
                }
            });
    };

    /**
     * Sort the options on a select element.
     */
    const sortSelectOptions = function sortSelectOptions($select) {
        const options = $select.children('option');
        options.detach().sort(function (a, b) {
            const at = $(a).text();
            const bt = $(b).text();
            if ('' === at || '*' === at) {
                return -1;
            } else if ('' === bt || '*' === bt) {
                return 1;
            }
            return (at > bt) ? 1 : ((at < bt) ? -1 : 0);
        });
        options.appendTo($select);
    };

    /**
     * Function that hides one or more bootstrap modals.
     */
    const hideModals = function hideModals(modals) {
        $.each(modals, function (index, modal) {
            if ($(modal).is(':visible')) {
                $(modal).modal('hide');
            }
        });
    };

    /**
     * Function to create an UUID.
     */
    const generateUUID = function generateUUID() {
        let d = new Date().getTime();
        if (window.performance && typeof window.performance.now === "function") {
            d += performance.now(); //use high-precision timer if available
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = (d + Math.random() * 16) % 16 | 0;
            d = Math.floor(d / 16);
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    };

    /**
     * Determine if a variable is numeric.
     * @param varToTest The variable to test
     * @returns {boolean} true when the variable is numeric, false otherwise.
     */
    const isNumeric = function isNumeric(varToTest) {
        return !isNaN(parseFloat(varToTest));
    };

    return {
        addAjaxHandlers: addAjaxHandlers,
        generateUUID: generateUUID,
        hideModals: hideModals,
        isInViewport: isInViewport,
        isNumeric: isNumeric,
        queryString: queryString,
        sortSelectOptions: sortSelectOptions

    };
})();

$(function () {
    // Hide all acl aware elements if this is a readonly page.
    if ('true' === commons.queryString['readonly']) {
        $("[data-element-type='acl-aware']").hide();
    }
    // Handle submenus.
    $('.dropdown-menu a.dropdown-toggle').on('click', function () {
        const $el = $(this);
        const $parent = $(this).offsetParent(".dropdown-menu");
        if (!$(this).next().hasClass('show') ) {
            $(this).parents('.dropdown-menu').first().find('.show').removeClass("show");
        }
        const $subMenu = $(this).next(".dropdown-menu");
        $subMenu.toggleClass( 'show' );

        $(this).parent("li").toggleClass('show');

        $(this).parents('li.nav-item.dropdown.show').on('hidden.bs.dropdown', function () {
            $('.dropdown-menu .show').removeClass("show");
        });

        if (!$parent.parent().hasClass('navbar-nav')) {
            $el.next().css({"top": $el[0].offsetTop, "left": $parent.outerWidth() - 4});
        }
        return false;
    });

    // Change all labels of required fields to italic
    $("[required='required']").each(function (index, item) {
        const id = $(item).attr('id');
        if (id) {
            $('label[for="' + id + '"]').css('font-style', 'italic');
        }
    });
});