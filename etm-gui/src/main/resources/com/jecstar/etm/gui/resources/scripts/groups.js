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

function buildGroupPage() {
	const $notifierSelect = $('<select>').addClass('form-control custom-select etm-notifier');
	let queryKeywords = null;

	$.when(
		$.ajax({
			type: 'GET',
			contentType: 'application/json',
			url: '../rest/settings/keywords/etm_event_all',
			cache: false,
			success: function (data) {
				if (!data || !data.keywords) {
					return;
				}
				queryKeywords = data.keywords;
				$('#input-filter-query').bind('keydown', function (event) {
					if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
						event.stopPropagation();
					}
				}).autocompleteFieldQuery({queryKeywords: queryKeywords});
			}
		}),
		$.ajax({
			type: 'GET',
			contentType: 'application/json',
			url: '../rest/settings/notifiers/basics',
			cache: false,
			success: function (data) {
				if (!data || !data.notifiers) {
					// No groups, remove the fieldset.
					$('#lnk-add-notifier').parent().remove();
					return;
				}
				$.each(data.notifiers, function (index, notifier) {
					$notifierSelect.append($('<option>').attr('value', notifier.name).text(notifier.name));
				});
				commons.sortSelectOptions($notifierSelect);
			}
		})
	).done(function () {
		$.ajax({
			type: 'GET',
			contentType: 'application/json',
			url: '../rest/settings/groups',
			cache: false,
			success: function (data) {
				if (!data) {
					return;
				}
				if (data.has_ldap) {
					$('#btn-confirm-import-group').show();
				}
				const $groupSelect = $('#sel-group');
				$.each(data.groups, function (index, group) {
					$groupSelect.append($('<option>').attr('value', group.name).text(group.name));
					groupMap[group.name] = group;
				});
				commons.sortSelectOptions($groupSelect);
				$groupSelect.val('');
			}
		});
	});

	const groupMap = {};
	$('#sel-group').on('change', function (event) {
		event.preventDefault();
		const groupData = groupMap[$(this).val()];
		if ('undefined' == typeof groupData) {
			resetValues();
			return;
		}
		$('#list-notifiers').empty();
		$('#input-group-name').val(groupData.name);
		$('#input-group-display-name').val(groupData.display_name);
		$('#input-filter-query').val(groupData.filter_query);
		$('#sel-filter-query-occurrence').val(groupData.filter_query_occurrence);
		$('#sel-always-show-correlated-events').val(groupData.always_show_correlated_events ? 'true' : 'false');
		$('#group-roles-container > label > input').prop('checked', false);
		if (groupData.roles) {
			$('#card-acl').find('select').val('none');
			$.each(groupData.roles, function (index, role) {
				$('#card-acl').find("option[value='" + role + "']").parent().val(role).trigger('change');
				;
			});
		}
		if (groupData.roles.indexOf('etm_event_read') !== -1 || groupData.roles.indexOf('etm_event_read_write') !== -1) {
			if (groupData.event_field_denies) {
				$.each(groupData.event_field_denies, function (index, deny) {
					$('#list-event-denies').append(createEventDenyRow(deny));
				});
			}
		}
		$('#dashboard-datasource-block').find("input[type='checkbox']").prop('checked', false);
		if (groupData.dashboard_datasources) {
			$('#dashboard-datasource-block').find("input[type='checkbox']").prop('checked', false);
			$.each(groupData.dashboard_datasources, function (index, ds) {
				$('#check-dashboard-datasource-' + ds).prop('checked', true);
			});
		}
		$('#signal-datasource-block').find("input[type='checkbox']").prop('checked', false);
		if (groupData.signal_datasources) {
			$('#signal-datasource-block').find("input[type='checkbox']").prop('checked', false);
			$.each(groupData.signal_datasources, function (index, ds) {
				$('#check-signal-datasource-' + ds).prop('checked', true);
			});
		}
		if (groupData.notifiers) {
			$.each(groupData.notifiers, function (index, notifierName) {
				$('#list-notifiers').append(createNotifierRow(notifierName));
			});
		}
		enableOrDisableButtons();
	});

	$('#btn-confirm-save-group').on('click', function (event) {
		event.preventDefault();
		if (!document.getElementById('group_form').checkValidity()) {
			return;
		}
		const groupName = $('#input-group-name').val();
		if (isGroupExistent(groupName)) {
			$('#overwrite-group-name').text(groupName);
			$('#modal-group-overwrite').modal();
		} else {
			saveGroup();
		}
	});

	$('#btn-save-group').on('click', function (event) {
		event.preventDefault();
		saveGroup();
	});

	$('#btn-confirm-remove-group').on('click', function (event) {
		event.preventDefault();
		$('#remove-group-name').text($('#input-group-name').val());
		$('#modal-group-remove').modal();
	});

	$('#btn-remove-group').on('click', function (event) {
		event.preventDefault();
		removeGroup($('#input-group-name').val());
	});

	$('#btn-confirm-import-group').on('click', function (event) {
		event.preventDefault();
		$("#sel-import-group").empty();
		$.ajax({
			type: 'GET',
			contentType: 'application/json',
			url: '../rest/settings/groups/ldap',
			cache: false,
			success: function (data) {
				if (!data) {
					return;
				}
				const $groupSelect = $('#sel-import-group');
				$.each(data.groups, function (index, group) {
					$groupSelect.append($('<option>').attr('value', group.name).text(group.name));
				});
				commons.sortSelectOptions($groupSelect);
				$groupSelect.val('');
				$('#modal-group-import').modal();
			}
		});
	});

	$('#btn-import-group').on('click', function (event) {
		event.preventDefault();
		const groupName = $("#sel-import-group").val();
		if (!groupName) {
			return false;
		}
		$.ajax({
			type: 'PUT',
			contentType: 'application/json',
			url: '../rest/settings/groups/ldap/import/' + encodeURIComponent(groupName),
			cache: false,
			success: function (group) {
				if (!group) {
					return;
				}
				// First the group if it is already present
				$('#sel-group > option').each(function () {
					if (group.name === $(this).attr('value')) {
						$(this).remove();
					}
				});
				// Now add the updated group
				$('#sel-group').append($('<option>').attr('value', group.name).text(group.name));
				commons.sortSelectOptions($('#sel-group'));
				groupMap[group.name] = group;
				$('#sel-group').val(group.name).trigger('change');
			}
		}).always(function () {
			commons.hideModals($('#modal-group-import'));
		});
	});

	$('#lnk-add-notifier').on('click', function (event) {
		event.preventDefault();
		$('#list-notifiers').append(createNotifierRow());
	});

	$('#lnk-add-deny').on('click', function (event) {
		event.preventDefault();
		$('#list-event-denies').append(createEventDenyRow());
	});

	$('#input-group-name').on('input', enableOrDisableButtons);

	$('#sel-event-acl').on('change', function (event) {
		event.preventDefault();
		if ('etm_event_read' === $(this).val() || 'etm_event_read_write' === $(this).val()) {
			$('#event-denies-container').show();
		} else {
			$('#event-denies-container').hide();
		}
	});

	function enableOrDisableButtons() {
		const groupName = $('#input-group-name').val();
		if (groupName) {
			$('#btn-confirm-save-group').removeAttr('disabled');
			if (isGroupExistent(groupName)) {
				$('#btn-confirm-remove-group').removeAttr('disabled');
			} else {
				$('#btn-confirm-remove-group').attr('disabled', 'disabled');
			}
		} else {
			$('#btn-confirm-save-group, #btn-confirm-remove-group').attr('disabled', 'disabled');
		}
	}

	function isGroupExistent(groupName) {
		return "undefined" != typeof groupMap[groupName];
	}

	function saveGroup() {
		const groupData = createGroupData();
		$.ajax({
			type: 'PUT',
			contentType: 'application/json',
			url: '../rest/settings/group/' + encodeURIComponent(groupData.name),
			cache: false,
			data: JSON.stringify(groupData),
			success: function (data) {
				if (!data) {
					return;
				}
				if (!isGroupExistent(groupData.name)) {
					const $groupSelect = $('#sel-group');
					$groupSelect.append($('<option>').attr('value', groupData.name).text(groupData.name));
					commons.sortSelectOptions($groupSelect);
				}
				groupMap[groupData.name] = groupData;
				commons.showNotification('Group \'' + groupData.name + '\' saved.', 'success');
			}
		}).always(function () {
			commons.hideModals($('#modal-group-overwrite'));
		});
	}

	function removeGroup(groupName) {
		$.ajax({
			type: 'DELETE',
			contentType: 'application/json',
			url: '../rest/settings/group/' + encodeURIComponent(groupName),
			cache: false,
			success: function (data) {
				if (!data) {
					return;
				}
				delete groupMap[groupName];
				$("#sel-group > option").filter(function () {
					return $(this).attr("value") === groupName;
				}).remove();
				commons.showNotification('Group \'' + groupName + '\' removed.', 'success');
			}
		}).always(function () {
			commons.hideModals($('#modal-group-remove'));
		});
	}

	function createNotifierRow(notifierName) {
		const $notifierRow = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
			$('<div>').addClass('input-group').append(
				$notifierSelect.clone(true),
				$('<div>').addClass('input-group-append').append(
					$('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {
						event.preventDefault();
						removeSelectedRow($(this));
					})
				)
			)
		);
		if (notifierName) {
			$notifierRow.find('.etm-notifier').val(notifierName)
		}
		return $notifierRow;
	}

	function createEventDenyRow(fieldName) {
		const $inputField = $('<input class="form-control form-control-sm">')
			.bind('keydown', function (event) {
				if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
					event.stopPropagation();
				}
			})
			.autocompleteFieldQuery(
				{
					queryKeywords: queryKeywords,
					mode: 'field'
				}
			);
		const $row = $('<li>').attr('style', 'margin-top: 5px; list-style-type: none;').append(
			$('<div>').addClass('input-group').append(
				$inputField,
				$('<div>').addClass('input-group-append').append(
					$('<button>').addClass('btn btn-sm btn-outline-secondary fa fa-times text-danger').attr('type', 'button').click(function (event) {
						event.preventDefault();
						removeSelectedRow($(this));
					})
				)
			)
		);
		if (fieldName) {
			$inputField.val(fieldName);
		}
		return $row;
	}

	function removeSelectedRow(anchor) {
		anchor.parent().parent().parent().remove();
	}

	function createGroupData() {
		const groupData = {
			name: $('#input-group-name').val(),
			display_name: $('#input-group-display-name').val() ? $('#input-group-display-name').val() : null,
			filter_query: $('#input-filter-query').val() ? $('#input-filter-query').val() : null,
			filter_query_occurrence: $('#sel-filter-query-occurrence').val(),
			always_show_correlated_events: $('#sel-always-show-correlated-events').val() == 'true' ? true : false,
			roles: [],
			dashboard_datasources: $('#dashboard-datasource-block')
				.find("input[type='checkbox']:checked")
				.map(function () {
					return $(this).val();
				}).get(),
			signal_datasources: $('#signal-datasource-block')
				.find("input[type='checkbox']:checked")
				.map(function () {
					return $(this).val();
				}).get(),
			notifiers: [],
			event_field_denies: []
		};
		$('#card-acl').find('select').each(function () {
			if ($(this).val() !== 'none') {
				groupData.roles.push($(this).val());
			}
		});
		$('.etm-notifier').each(function () {
			const notifierName = $(this).val();
			if (-1 === groupData.notifiers.indexOf(notifierName)) {
				groupData.notifiers.push(notifierName);
			}
		});
		if (groupData.roles.indexOf('etm_event_read') !== -1 || groupData.roles.indexOf('etm_event_read_write') !== -1) {
			$('#list-event-denies').find('input').each(function () {
				const val = $(this).val();
				if (val.trim().length > 0) {
					groupData.event_field_denies.push(val.trim());
				}
			});
		}
		return groupData;
	}

	function resetValues() {
		document.getElementById('group_form').reset();
		$('#sel-event-acl').trigger('change');
		enableOrDisableButtons();
	}
}