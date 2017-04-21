function buildAuditLogPage() {

	$.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: '../rest/audit/keywords/etm_audit_all',
        success: function(data) {
            if (!data || !data.keywords) {
                return;
            }
            $('#input-query-string')
            .on('input', function( event ) {
                if ($(this).val()) {
                	$('#btn-search').removeAttr("disabled");
                } else {
                	$('#btn-search').attr('disabled', 'disabled');
                }
            })
            .bind('keydown', function( event ) {
                if (event.keyCode === $.ui.keyCode.ESCAPE && $(this).autocomplete('instance').menu.active) {
                    event.stopPropagation();
                }
            })
            .autocompleteFieldQuery(
            	{
            		queryKeywords: data.keywords
            	}
            );
        }
    })

    
    
    
    function enableOrDisableSearchButton() {
        if (!$('#query-string').val()) {
            $('#btn-search').attr('disabled', 'disabled');
        } else {
            $('#btn-search').removeAttr("disabled");
        }
    }
	
}