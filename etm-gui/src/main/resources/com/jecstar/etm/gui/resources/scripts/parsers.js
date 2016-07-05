function buildParserPage() {
	var parserMap = {};
	$('#input-parser-type').change(function() {
		$('#grp-fixed_position,#grp-fixed_value,#grp-jsonpath,#grp-xpath,#grp-xslt').hide();
		$('#grp-' +$(this).val()).show(); 
	});

	$('#sel-parser').change(function() {
		var parserData = parserMap[$(this).val()];
		if ("undefined" == typeof parserData) {
			$('#input-parser-name').val('');
			$('#input-parser-type').val('fixed_position').change();
			$('#input-fixed_position-line').val(1)
			$('#input-fixed_position-start-ix').val(1);
			$('#input-fixed_position-end-ix').val(2)
			return;
		}
		
		$('#input-parser-name').val(parserData.name);
		$('#input-parser-type').val(parserData.type).change();
		if ('fixed_position' == parserData.type) {
			$('#input-fixed_position-line').val(parserData.line + 1)
			$('#input-fixed_position-start-ix').val(parserData.start_ix + 1);
			$('#input-fixed_position-end-ix').val(parserData.end_ix + 1)
		} else if ('fixed_value' == parserData.type) {
			$('#input-fixed-value').val(parserData.value);
		} else if ('jsonpath' == parserData.type) {
			$('#input-jsonpath-expression').val(parserData.expression);
		} else if ('xpath' == parserData.type) {
			$('#input-xpath-expression').val(parserData.expression);
		} else if ('xslt' == parserData.type) {
			$('#input-xslt-template').val(parserData.template);
		}
	});	
	
	$.ajax({
	    type: 'GET',
	    contentType: 'application/json',
	    url: '../rest/settings/parsers',
	    success: function(data) {
	        if (!data) {
	            return;
	        }
	        $parserSelect = $('#sel-parser');
	        $.each(data.parsers.sort(sortParsersByName), function(index, parser) {
	        	$parserSelect.append($('<option>').attr('value', parser.name).text(parser.name));
	        	parserMap[parser.name] = parser;
	        });
	    }
	});
	
	function sortParsersByName(a, b){
	  return ((a.name < b.name) ? -1 : ((a.name > b.name) ? 1 : 0));
	}

}