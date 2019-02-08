function buildNotifiersPage() {
    var Base64={_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9+/=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/rn/g,"n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}
    var notifierMap = {};

	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/notifiers',
	    cache: false,
	    success: function(data) {
	        if (!data) {
	            return;
	        }
            const $notifierSelect = $('#sel-notifier');
	        $.each(data.notifiers, function(index, notifier) {
	        	$notifierSelect.append($('<option>').attr('value', notifier.name).text(notifier.name));
	        	notifierMap[notifier.name] = notifier;
	        });
            commons.sortSelectOptions($notifierSelect)
	        $notifierSelect.val('');
	    }
	});

	$('input[data-required]').on('input', enableOrDisableButtons);

    $('#sel-notifier').change(function(event) {
        event.preventDefault();
        var notifierData = notifierMap[$(this).val()];
        resetValues();
        if ('undefined' == typeof notifierData) {
            return;
        }
        $('#input-notifier-name').val(notifierData.name);
        $('#sel-notifier-type').val(notifierData.type).trigger('change');
        if ('EMAIL' == notifierData.type) {
            $('#input-smtp-host').val(notifierData.host);
            $('#input-smtp-port').val(notifierData.port);
            $('#sel-smtp-connection-security').val(notifierData.smtp_connection_security);
            $('#input-smtp-username').val(notifierData.username);
            $('#input-smtp-password').val(decode(notifierData.password));
            $('#input-from-address').val(notifierData.smtp_from_address);
            $('#input-from-name').val(notifierData.smtp_from_name);
        } else if ('SNMP' == notifierData.type) {
            $('#input-snmp-host').val(notifierData.host);
            $('#input-snmp-port').val(notifierData.port);
            $('#sel-snmp-version').val(notifierData.snmp_version).trigger('change');
            if ('V1' == notifierData.snmp_version || 'V2C' == notifierData.snmp_version) {
                $('#input-snmp-community').val(decode(notifierData.snmp_community));
            } else if ('V3' == notifierData.snmp_version) {
                $('#input-snmp-security-name').val(notifierData.username);
                $('#sel-snmp-authentication-protocol').val(notifierData.snmp_authentication_protocol);
                $('#input-snmp-authentication-passphrase').val(decode(notifierData.password));
                $('#sel-snmp-privacy-protocol').val(notifierData.snmp_privacy_protocol);
                $('#input-snmp-privacy-passphrase').val(decode(notifierData.snmp_privacy_passphrase));
            }
            $('#sel-snmp-authentication-protocol').trigger('change');
            $('#sel-snmp-privacy-protocol').trigger('change');
        }
        enableOrDisableButtons();
    });

	$('#sel-notifier-type').on('change', function(event) {
        event.preventDefault();
	    if ('ETM_BUSINESS_EVENT' == $(this).val()) {
            $('#email-fields, #snmp-fields').hide();
	        $('#business-event-fields').show();
	    } else if ('EMAIL' == $(this).val()) {
            $('#business-event-fields, #snmp-fields').hide();
	        $('#email-fields').show();
        } else if ('SNMP' == $(this).val()) {
            $('#business-event-fields, #email-fields').hide();
            $('#snmp-fields').show();
        }
	    enableOrDisableButtons();
	});

    $('#sel-snmp-version').on('change', function (event) {
        event.preventDefault();
        if ('V1' == $(this).val() || 'V2C' == $(this).val()) {
            $('#group-snmp-security-name' +
                ', #group-snmp-authentication-protocol' +
                ', #group-snmp-authentication-passphrase' +
                ', #group-snmp-privacy-protocol' +
                ', #group-input-snmp-privacy-passphrase').hide();
            $('#group-snmp-community').show();
        } else if ('V3' == $(this).val()) {
            $('#group-snmp-community').hide();
            $('#group-snmp-security-name' +
                ', #group-snmp-authentication-protocol').show();

            $('#sel-snmp-authentication-protocol').trigger('change');
        }
    });

    $('#sel-snmp-authentication-protocol').on('change', function (event) {
        event.preventDefault();
        if ($(this).val()) {
            $('#group-snmp-authentication-passphrase, #group-snmp-privacy-protocol').show();
            $('#sel-snmp-privacy-protocol').trigger('change');
        } else {
            $('#group-snmp-authentication-passphrase, #group-snmp-privacy-protocol, #group-input-snmp-privacy-passphrase').hide();
        }
    });

    $('#sel-snmp-privacy-protocol').on('change', function (event) {
        event.preventDefault();
        if ($(this).val()) {
            $('#group-input-snmp-privacy-passphrase').show();
        } else {
            $('#group-input-snmp-privacy-passphrase').hide();
        }
    });

    $('#btn-confirm-save-notifier').click(function(event) {
        if (!document.getElementById('notifier_form').checkValidity()) {
            return;
        }
        event.preventDefault();
        var notifierName = $('#input-notifier-name').val();
        if (isNotifierExistent(notifierName)) {
            $('#overwrite-notifier-name').text(notifierName);
            $('#modal-notifier-overwrite').modal();
        } else {
            saveNotifier();
        }
    });

    $('#btn-save-notifier').click(function(event) {
        saveNotifier();
    });

    $('#btn-confirm-remove-notifier').click(function(event) {
        event.preventDefault();
        $('#remove-notifier-name').text($('#input-notifier-name').val());
        $('#modal-notifier-remove').modal();
    });

    $('#btn-remove-notifier').click(function(event) {
        removeNotifier($('#input-notifier-name').val());
    });

    function enableOrDisableButtons() {
        // Set the required constrains on the visible fields.
        $('#notifier_form :input[data-required]:visible').attr('required', 'required');
        // Remove the required constrains on the hidden fields
        $('#notifier_form :input[data-required]:hidden').removeAttr('required');

        if (document.getElementById('notifier_form').checkValidity()) {
            $('#btn-confirm-save-notifier').removeAttr('disabled');
        } else {
            $('#btn-confirm-save-notifier').attr('disabled', 'disabled');
        }
        var notifierName = $('#input-notifier-name').val();
        if (notifierName && isNotifierExistent(notifierName)) {
            $('#btn-confirm-remove-notifier').removeAttr('disabled');
        } else {
            $('#btn-confirm-remove-notifier').attr('disabled', 'disabled');
        }
    }

    function saveNotifier() {
        var notifierData = createNotifierData();
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/notifier/' + encodeURIComponent(notifierData.name),
            cache: false,
            data: JSON.stringify(notifierData),
            success: function(data) {
                if (!data) {
                    return;
                }
                if (!isNotifierExistent(notifierData.name)) {
                    const $notifierSelect = $('#sel-notifier');
                    $notifierSelect.append($('<option>').attr('value', notifierData.name).text(notifierData.name));
                    commons.sortSelectOptions($notifierSelect);
                }
                notifierMap[notifierData.name] = notifierData;
                $('#notifiers_infoBox').text('Notifier \'' + notifierData.name + '\' saved.').show('fast').delay(5000).hide('fast');
                enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-notifier-overwrite'));
        });
    }

    function removeNotifier(notifierName) {
        $.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/notifier/' + encodeURIComponent(notifierName),
            cache: false,
            success: function(data) {
                if (!data) {
                    return;
                }
                delete notifierMap[notifierName];
                $("#sel-notifier > option").filter(function(i){
                   return $(this).attr("value") == notifierName;
                }).remove();
                $('#notifiers_infoBox').text('Notifier \'' + notifierName + '\' removed.').show('fast').delay(5000).hide('fast');
                enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-notifier-remove'));
        });
    }

    function createNotifierData() {
        var notifierData = {
            name: $('#input-notifier-name').val(),
            type: $('#sel-notifier-type').val()
        };
        if ('EMAIL' == notifierData.type) {
            notifierData.host = $('#input-smtp-host').val();
            notifierData.port = Number($('#input-smtp-port').val());
            notifierData.smtp_connection_security = $('#sel-smtp-connection-security').val() ? $('#sel-smtp-connection-security').val() : null;
            notifierData.username = $('#input-smtp-username').val() ? $('#input-smtp-username').val() : null;
            notifierData.password = encode($('#input-smtp-password').val());
            notifierData.smtp_from_address = $('#input-from-address').val() ? $('#input-from-address').val() : null;
            notifierData.smtp_from_name = $('#input-from-name').val() ? $('#input-from-name').val() : null;
        } else if ('SNMP' == notifierData.type) {
            notifierData.host = $('#input-snmp-host').val();
            notifierData.port = Number($('#input-snmp-port').val());
            notifierData.snmp_version = $('#sel-snmp-version').val();
            if ('V1' == notifierData.snmp_version || 'V2C' == notifierData.snmp_version) {
                notifierData.snmp_community = encode($('#input-snmp-community').val())
            } else if ('V3' == notifierData.snmp_version) {
                notifierData.username = $('#input-snmp-security-name').val() ? $('#input-snmp-security-name').val() : null;
                if ($('#sel-snmp-authentication-protocol').val()) {
                    notifierData.snmp_authentication_protocol = $('#sel-snmp-authentication-protocol').val();
                    notifierData.password = encode($('#input-snmp-authentication-passphrase').val());
                    if ($('#sel-snmp-privacy-protocol').val()) {
                        notifierData.snmp_privacy_protocol = $('#sel-snmp-privacy-protocol').val();
                        notifierData.snmp_privacy_passphrase = encode($('#input-snmp-privacy-passphrase').val());
                    }
                }
            }
        }
        return notifierData;
    }

    function isNotifierExistent(notifierName) {
        return "undefined" != typeof notifierMap[notifierName];
    }

    function decode(data) {
        if (!data) {
            return null;
        }
        for (i = 0; i < 7; i++) {
            data = Base64.decode(data);
        }
        return data;
    }

    function encode(data) {
        if (!data) {
            return null;
        }
        for (i = 0; i < 7; i++) {
            data = Base64.encode(data);
        }
        return data;
    }

    function resetValues() {
        document.getElementById('notifier_form').reset();
        $('#sel-notifier-type').trigger('change');
        enableOrDisableButtons();
    }
}