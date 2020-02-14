/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

function buildParserPage() {
    "use strict";
    const parserMap = {};
    $('#sel-parser-type').on('change', function () {
        $('#grp-copy_value, #grp-fixed_position, #grp-fixed_value, #grp-javascript, #grp-jsonpath, #grp-regex, #grp-xpath, #grp-xslt').hide();
        // Disable all input fields otherwise form validation will fail.
        $('#grp-fixed_position input, #grp-fixed_value input, #grp-javascript input, #grp-javascript textarea, #grp-jsonpath input, #grp-regex input, #grp-regex select, #grp-xpath input ,#grp-xslt textarea').attr('disabled', 'disabled');
        $('#grp-' + $(this).val() + ' input').removeAttr('disabled');
        $('#grp-' + $(this).val() + ' select').removeAttr('disabled');
        $('#grp-' + $(this).val() + ' textarea').removeAttr('disabled');
        $('#grp-' + $(this).val()).show();
    });

    $('#sel-parser').on('change', function (event) {
        event.preventDefault();
        const parserData = parserMap[$(this).val()];
        if ('undefined' == typeof parserData) {
            resetValues();
            return;
        }

        $('#input-parser-name').val(parserData.name);
        $('#sel-parser-type').val(parserData.type).change();
        if ('fixed_position' === parserData.type) {
            $('#input-fixed_position-line').val(parserData.line + 1)
            $('#input-fixed_position-start-ix').val(parserData.start_ix + 1);
            $('#input-fixed_position-end-ix').val(parserData.end_ix + 1)
        } else if ('fixed_value' === parserData.type) {
            $('#input-fixed-value').val(parserData.value);
        } else if ('javascript' === parserData.type) {
            $('#input-javascript-main_function').val(parserData.main_function);
            $('#input-javascript-script').val(parserData.expression);
        } else if ('jsonpath' === parserData.type) {
            $('#input-jsonpath-expression').val(parserData.expression);
        } else if ('regex' === parserData.type) {
            $('#input-regex-expression').val(parserData.expression);
            $('#input-regex-group').val(parserData.group);
            $('#sel-regex-canonical-equivalence').val(parserData.canonical_equivalence ? 'true' : 'false');
            $('#sel-regex-case-insensitive').val(parserData.case_insensitive ? 'true' : 'false');
            $('#sel-regex-dotall').val(parserData.dotall ? 'true' : 'false');
            $('#sel-regex-literal').val(parserData.literal ? 'true' : 'false');
            $('#sel-regex-multiline').val(parserData.multiline ? 'true' : 'false');
            $('#sel-regex-unicode-case').val(parserData.unicode_case ? 'true' : 'false');
            $('#sel-regex-unicode-character-class').val(parserData.unicode_character_class ? 'true' : 'false');
            $('#sel-regex-unix-lines').val(parserData.unix_lines ? 'true' : 'false');
        } else if ('xpath' === parserData.type) {
            $('#input-xpath-expression').val(parserData.expression);
        } else if ('xslt' === parserData.type) {
            $('#input-xslt-template').val(parserData.template);
        }
        enableOrDisableButtons();
    });

    $('#btn-confirm-save-parser').on('click', function (event) {
        if (!document.getElementById('parser_form').checkValidity()) {
            return;
        }
        event.preventDefault();
        const parserName = $('#input-parser-name').val();
        if (isParserExistent(parserName)) {
            $('#overwrite-parser-name').text(parserName);
            $('#modal-parser-overwrite').modal();
        } else {
            saveParser();
        }
    });

    $('#btn-save-parser').on('click', function (event) {
        saveParser();
    });

    $('#btn-confirm-remove-parser').on('click', function (event) {
        event.preventDefault();
        $('#remove-parser-name').text($('#input-parser-name').val());
        $('#modal-parser-remove').modal();
    });

    $('#btn-remove-parser').on('click', function (event) {
        removeParser($('#input-parser-name').val());
    });


    $('#input-parser-name').on('input', enableOrDisableButtons);

    $.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/settings/parsers',
        cache: false,
        success: function (data) {
            if (!data) {
                return;
            }
            const $parserSelect = $('#sel-parser');
            $.each(data.parsers, function (index, parser) {
                $parserSelect.append($('<option>').attr('value', parser.name).text(parser.name));
                parserMap[parser.name] = parser;
            });
            commons.sortSelectOptions($parserSelect)
            $parserSelect.val('');
        }
    });

    function enableOrDisableButtons() {
        const parserName = $('#input-parser-name').val();
        if (parserName) {
            $('#btn-confirm-save-parser').removeAttr('disabled');
            if (isParserExistent(parserName)) {
                $('#btn-confirm-remove-parser').removeAttr('disabled');
            } else {
                $('#btn-confirm-remove-parser').attr('disabled', 'disabled');
            }
        } else {
            $('#btn-confirm-save-parser, #btn-confirm-remove-parser').attr('disabled', 'disabled');
        }
    }

    function isParserExistent(name) {
        return "undefined" != typeof parserMap[name];
    }

    function saveParser() {
        const parserData = createParserData();
        $.ajax({
            type: 'PUT',
            contentType: 'application/json',
            url: '../rest/settings/parser/' + encodeURIComponent(parserData.name),
            cache: false,
            data: JSON.stringify(parserData),
            success: function (data) {
                if (!data) {
                    return;
                }
                if (!isParserExistent(parserData.name)) {
                    const $parserSelect = $('#sel-parser');
                    $parserSelect.append($('<option>').attr('value', parserData.name).text(parserData.name));
                    commons.sortSelectOptions($parserSelect);
                }
                parserMap[parserData.name] = parserData;
                commons.showNotification('Parser \'' + parserData.name + '\' saved.', 'success');
            }
        }).always(function () {
            commons.hideModals($('#modal-parser-overwrite'));
        });
    }

    function removeParser(parserName) {
        $.ajax({
            type: 'DELETE',
            contentType: 'application/json',
            url: '../rest/settings/parser/' + encodeURIComponent(parserName),
            cache: false,
            success: function (data) {
                if (!data) {
                    return;
                }
                delete parserMap[parserName];
                $("#sel-parser > option").filter(function (i) {
                    return $(this).attr("value") === parserName;
                }).remove();
                commons.showNotification('Parser \'' + parserName + '\' removed.', 'success');
            }
        }).always(function () {
            commons.hideModals($('#modal-parser-remove'));
        });
    }

    function createParserData() {
        const parserData = {
            name: $('#input-parser-name').val(),
            type: $('#sel-parser-type').val()
        };
        if ('copy_value' === parserData.type) {
        } else if ('fixed_position' === parserData.type) {
            parserData['line'] = Number($('#input-fixed_position-line').val()) - 1;
            parserData['start_ix'] = Number($('#input-fixed_position-start-ix').val()) - 1;
            parserData['end_ix'] = Number($('#input-fixed_position-end-ix').val()) - 1;
        } else if ('fixed_value' === parserData.type) {
            parserData['value'] = $('#input-fixed-value').val();
        } else if ('javascript' === parserData.type) {
            parserData['main_function'] = $('#input-javascript-main_function').val();
            parserData['expression'] = $('#input-javascript-script').val();
        } else if ('jsonpath' === parserData.type) {
            parserData['expression'] = $('#input-jsonpath-expression').val();
        } else if ('regex' === parserData.type) {
            parserData['expression'] = $('#input-regex-expression').val();
            parserData['group'] = Number($('#input-regex-group').val());
            parserData['canonical_equivalence'] = $('#sel-regex-canonical-equivalence').val() == 'true' ? true : false;
            parserData['case_insensitive'] = $('#sel-regex-case-insensitive').val() == 'true' ? true : false;
            parserData['dotall'] = $('#sel-regex-dotall').val() == 'true' ? true : false;
            parserData['literal'] = $('#sel-regex-literal').val() == 'true' ? true : false;
            parserData['multiline'] = $('#sel-regex-multiline').val() == 'true' ? true : false;
            parserData['unicode_case'] = $('#sel-regex-unicode-case').val() == 'true' ? true : false;
            parserData['unicode_character_class'] = $('#sel-regex-unicode-character-class').val() == 'true' ? true : false;
            parserData['unix_lines'] = $('#sel-regex-unix-lines').val() == 'true' ? true : false;
        } else if ('xpath' === parserData.type) {
            parserData['expression'] = $('#input-xpath-expression').val();
        } else if ('xslt' === parserData.type) {
            parserData['template'] = $('#input-xslt-template').val();
        }
        return parserData;
    }

    function resetValues() {
        document.getElementById('parser_form').reset();
        $('#sel-parser-type').val('copy_value').change();
        enableOrDisableButtons();
    }
}