function buildParserPage() {
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/parsers',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $parserSelect = $('#sel_parser');
	        
	        $.each(data.parsers.sort(sortParsersByName), function(index, parser) {
	        	$parserSelect.append($('<option>').attr('value', parser.name).text(parser.name));
	        });
	    }
	});
	
	function sortParsersByName(a, b){
	  return ((a.name < b.name) ? -1 : ((a.name > b.name) ? 1 : 0));
	}

}