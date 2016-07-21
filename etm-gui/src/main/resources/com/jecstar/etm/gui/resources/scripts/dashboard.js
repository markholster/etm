$(document).on('click', '#dashboard-name', function(){
	$('#dashboard-name').on('click.disabled', false);
	var oldText = $(this).text();
	$('#dashboard-name').text('');
	var inputElement = $('<input>').addClass('form-control').attr('type', 'text').val(oldText);
	inputElement.bind('keydown focusout', function( event ) {
		if ('focusout' === event.type || event.keyCode === $.ui.keyCode.ENTER) {
        	if (inputElement.val() && inputElement.val().trim().length > 0) {
        		// TODO controle of een dashboard al bestaat
        		inputElement.remove();
        		$('#dashboard-name').empty().text(inputElement.val()).off('click.disabled');
        	}			
		} else if ( event.keyCode === $.ui.keyCode.ESCAPE) {
        	inputElement.remove();
        	$('#dashboard-name').empty().text(oldText).off('click.disabled');
        }
    });
    $('#dashboard-name').append(inputElement);
	inputElement.focus();
});

function loadDashboard(name) {

}

function createNewDashboard() {

}