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
            $('.navbar-brand').addClass('pulse');
        }).ajaxStop(function () {
            if (stopFunction) {
                stopFunction();
            }
            if (jQuery.active === 0) {
                $('.navbar-brand').removeClass('pulse');
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
     * Function to create an UUIDv4.
     */
    const generateUUID = function generateUUID() {
        return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
            (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
        );
    };

    /**
     * Determine if a variable is numeric.
     * @param varToTest The variable to test
     * @returns {boolean} true when the variable is numeric, false otherwise.
     */
    const isNumeric = function isNumeric(varToTest) {
        return !isNaN(parseFloat(varToTest));
    };

    /**
     * Shows a notification to the user.
     * @param message The message to show.
     * @param type The notification type, for example 'danger'.
     */
    const showNotification = function showNotification(message, type) {
        $.notify(
            {message: message}
            , {type: type});
    };

    return {
        addAjaxHandlers: addAjaxHandlers,
        generateUUID: generateUUID,
        hideModals: hideModals,
        isInViewport: isInViewport,
        isNumeric: isNumeric,
        queryString: queryString,
        showNotification: showNotification,
        sortSelectOptions: sortSelectOptions

    };
})();

$(function () {
    // Hide all acl aware elements if this is a readonly page.
    if ('true' === commons.queryString['readonly']) {
        $("[data-element-type='acl-aware']").hide();
    }

    // Change all labels of required fields to italic
    $("[required='required']").each(function (index, item) {
        const id = $(item).attr('id');
        if (id) {
            $('label[for="' + id + '"]').css('font-style', 'italic');
        }
    });
});