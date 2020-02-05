// Utility class for common ETM functions. This script should be loaded after jQuery.
const commons = (function () {
    "use strict";

    /**
     * Method to retrieve query string parameters.
     */
    const queryString = (function (a) {
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
        return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, function (c) {
            (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
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

    /**
     * Base64 class to support encoding and decoding of base64 strings.
     *
     * @type {{encode: (function(*=): (string|string)), _utf8_encode: (function(*): (string|string)), decode: (function(*): (*|string)), _utf8_decode: (function(*): (string|string)), _keyStr: string}}
     */
    const Base64 = {
        _keyStr: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",
        encode: function (e) {
            let t = "";
            let n, r, i, s, o, u, a;
            let f = 0;

            e = Base64._utf8_encode(e);

            while (f < e.length) {
                n = e.charCodeAt(f++);
                r = e.charCodeAt(f++);
                i = e.charCodeAt(f++);
                s = n >> 2;
                o = (n & 3) << 4 | r >> 4;
                u = (r & 15) << 2 | i >> 6
                ;a = i & 63;
                if (isNaN(r)) {
                    u = a = 64
                } else if (isNaN(i)) {
                    a = 64
                }
                t = t + Base64._keyStr.charAt(s) + Base64._keyStr.charAt(o) + Base64._keyStr.charAt(u) + Base64._keyStr.charAt(a)
            }
            return t
        },
        decode: function (e) {
            let t = "";
            let n, r, i;
            let s, o, u, a;
            let f = 0;

            e = e.replace(/[^A-Za-z0-9+/=]/g, "");

            while (f < e.length) {
                s = Base64._keyStr.indexOf(e.charAt(f++));
                o = Base64._keyStr.indexOf(e.charAt(f++));
                u = Base64._keyStr.indexOf(e.charAt(f++));
                a = Base64._keyStr.indexOf(e.charAt(f++));
                n = s << 2 | o >> 4;
                r = (o & 15) << 4 | u >> 2;
                i = (u & 3) << 6 | a;
                t = t + String.fromCharCode(n);

                if (u !== 64) {
                    t = t + String.fromCharCode(r)
                }
                if (a !== 64) {
                    t = t + String.fromCharCode(i)
                }
            }
            t = Base64._utf8_decode(t);
            return t
        },
        _utf8_encode: function (e) {
            e = e.replace(/rn/g, "n");
            let t = "";
            for (let n = 0; n < e.length; n++) {
                let r = e.charCodeAt(n);
                if (r < 128) {
                    t += String.fromCharCode(r)
                } else if (r > 127 && r < 2048) {
                    t += String.fromCharCode(r >> 6 | 192);
                    t += String.fromCharCode(r & 63 | 128);
                } else {
                    t += String.fromCharCode(r >> 12 | 224);
                    t += String.fromCharCode(r >> 6 & 63 | 128);
                    t += String.fromCharCode(r & 63 | 128)
                }
            }
            return t
        },
        _utf8_decode: function (e) {
            let t = "";
            let n = 0;
            let r = 0;
            let c1 = 0;
            while (n < e.length) {
                r = e.charCodeAt(n);
                if (r < 128) {
                    t += String.fromCharCode(r);
                    n++
                } else if (r > 191 && r < 224) {
                    c1 = e.charCodeAt(n + 1);
                    t += String.fromCharCode((r & 31) << 6 | c1 & 63);
                    n += 2
                } else {
                    c1 = e.charCodeAt(n + 1);
                    const c2 = e.charCodeAt(n + 2);
                    t += String.fromCharCode((r & 15) << 12 | (c1 & 63) << 6 | c2 & 63);
                    n += 3
                }
            }
            return t
        }
    };

    return {
        addAjaxHandlers: addAjaxHandlers,
        base64Decode: Base64.decode,
        base64Encode: Base64.encode,
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