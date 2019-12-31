function buildNotifiersPage() {
    "use strict";
    let notifierMap = {};

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
            commons.sortSelectOptions($notifierSelect);
	        $notifierSelect.val('');
	    }
	});

	$('input[data-required]').on('input', enableOrDisableButtons);

    $('#sel-notifier').change(function(event) {
        event.preventDefault();
        const notifierData = notifierMap[$(this).val()];
        resetValues();
        if ('undefined' == typeof notifierData) {
            return;
        }
        $('#input-notifier-name').val(notifierData.name);
        $('#sel-notifier-type').val(notifierData.type).trigger('change');
        if ('EMAIL' === notifierData.type) {
            $('#input-smtp-host').val(notifierData.host);
            $('#input-smtp-port').val(notifierData.port);
            $('#sel-smtp-connection-security').val(notifierData.smtp_connection_security);
            $('#input-smtp-username').val(notifierData.username);
            $('#input-smtp-password').val(decode(notifierData.password));
            $('#input-from-address').val(notifierData.smtp_from_address);
            $('#input-from-name').val(notifierData.smtp_from_name);
        } else if ('SNMP' === notifierData.type) {
            $('#input-snmp-host').val(notifierData.host);
            $('#input-snmp-port').val(notifierData.port);
            $('#sel-snmp-version').val(notifierData.snmp_version).trigger('change');
            if ('V1' === notifierData.snmp_version || 'V2C' === notifierData.snmp_version) {
                $('#input-snmp-community').val(decode(notifierData.snmp_community));
            } else if ('V3' === notifierData.snmp_version) {
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
        if ('ETM_BUSINESS_EVENT' === $(this).val()) {
            $('#email-fields, #snmp-fields').hide();
	        $('#business-event-fields').show();
        } else if ('EMAIL' === $(this).val()) {
            $('#business-event-fields, #snmp-fields').hide();
	        $('#email-fields').show();
        } else if ('SNMP' === $(this).val()) {
            $('#business-event-fields, #email-fields').hide();
            $('#snmp-fields').show();
        }
	    enableOrDisableButtons();
	});

    $('#sel-snmp-version').on('change', function (event) {
        event.preventDefault();
        if ('V1' === $(this).val() || 'V2C' === $(this).val()) {
            $('#group-snmp-security-name' +
                ', #group-snmp-authentication-protocol' +
                ', #group-snmp-authentication-passphrase' +
                ', #group-snmp-privacy-protocol' +
                ', #group-input-snmp-privacy-passphrase').hide();
            $('#group-snmp-community').show();
        } else if ('V3' === $(this).val()) {
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

    $('#btn-confirm-save-notifier').on('click', function (event) {
        if (!document.getElementById('notifier_form').checkValidity()) {
            return;
        }
        event.preventDefault();
        const notifierName = $('#input-notifier-name').val();
        if (isNotifierExistent(notifierName)) {
            $('#overwrite-notifier-name').text(notifierName);
            $('#modal-notifier-overwrite').modal();
        } else {
            saveNotifier(true);
        }
    });

    $('#btn-save-notifier').on('click', function (event) {
        event.preventDefault();
        saveNotifier(true);
    });

    $('#btn-save-notifier-without-connection-test').on('click', function (event) {
        event.preventDefault();
        commons.hideModals($('#modal-notifier-connection-error'));
        saveNotifier(false);
    });

    $('#btn-confirm-remove-notifier').on('click', function (event) {
        event.preventDefault();
        $('#remove-notifier-name').text($('#input-notifier-name').val());
        $('#modal-notifier-remove').modal();
    });

    $('#btn-remove-notifier').on('click', function (event) {
        event.preventDefault();
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

    function saveNotifier(testConnection) {
        const notifierData = createNotifierData(testConnection);
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
                if (data.status === 'success') {
                    if (!isNotifierExistent(notifierData.name)) {
                        const $notifierSelect = $('#sel-notifier');
                        $notifierSelect.append($('<option>').attr('value', notifierData.name).text(notifierData.name));
                        commons.sortSelectOptions($notifierSelect);
                    }
                    notifierMap[notifierData.name] = notifierData;
                    commons.showNotification('Notifier \'' + notifierData.name + '\' saved.', 'success');
                    enableOrDisableButtons();
                } else {
                    commons.hideModals($('#modal-notifier-overwrite'));
                    $('#modal-notifier-connection-message').text(data.reason);
                    $('#modal-notifier-connection-error').modal();
                }
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
                $("#sel-notifier > option").filter(function () {
                    return $(this).attr("value") === notifierName;
                }).remove();
                commons.showNotification('Notifier \'' + notifierName + '\' removed.', 'success');
                enableOrDisableButtons();
            }
        }).always(function () {
            commons.hideModals($('#modal-notifier-remove'));
        });
    }

    function createNotifierData(testConnection) {
        const notifierData = {
            name: $('#input-notifier-name').val(),
            type: $('#sel-notifier-type').val(),
            test_connection: !!testConnection
        };
        if ('EMAIL' === notifierData.type) {
            notifierData.host = $('#input-smtp-host').val();
            notifierData.port = Number($('#input-smtp-port').val());
            notifierData.smtp_connection_security = $('#sel-smtp-connection-security').val() ? $('#sel-smtp-connection-security').val() : null;
            notifierData.username = $('#input-smtp-username').val() ? $('#input-smtp-username').val() : null;
            notifierData.password = encode($('#input-smtp-password').val());
            notifierData.smtp_from_address = $('#input-from-address').val() ? $('#input-from-address').val() : null;
            notifierData.smtp_from_name = $('#input-from-name').val() ? $('#input-from-name').val() : null;
        } else if ('SNMP' === notifierData.type) {
            notifierData.host = $('#input-snmp-host').val();
            notifierData.port = Number($('#input-snmp-port').val());
            notifierData.snmp_version = $('#sel-snmp-version').val();
            if ('V1' === notifierData.snmp_version || 'V2C' === notifierData.snmp_version) {
                notifierData.snmp_community = encode($('#input-snmp-community').val())
            } else if ('V3' === notifierData.snmp_version) {
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
        for (let i = 0; i < 7; i++) {
            data = commons.base64Decode(data);
        }
        return data;
    }

    function encode(data) {
        if (!data) {
            return null;
        }
        for (let i = 0; i < 7; i++) {
            data = commons.base64Encode(data);
        }
        return data;
    }

    function resetValues() {
        document.getElementById('notifier_form').reset();
        $('#sel-notifier-type').trigger('change');
        enableOrDisableButtons();
    }
}