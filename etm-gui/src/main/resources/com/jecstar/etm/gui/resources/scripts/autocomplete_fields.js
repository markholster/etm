(function ($) {
	// TODO! Autocomplete on mobile devices.
	$.fn.autocompleteFieldQuery = function(options) {
	 
	    var queryOperators = ['AND', 'AND NOT', 'OR'];
	    var queryForFields = ['_exists_', '_missing_', '_type'];
		
        var settings = $.extend({
            keywordIndexFilter: function(index) {
            	return false;
            },
            keywordGroupFilter: function(index, group) {
            	return false;
            },
            keywordFilter: function(index, group, keyword) {
            	return false;
            },            
            filter: function(keywords) {
            	if (keywords == null) {
            		return null;
            	}
	            var values = [];
	            
            	$.each(keywords, function(ix, keywordGroup) {
            		if (settings.keywordIndexFilter(keywordGroup.index)) {
            			return true;
            		}
            		if (settings.keywordGroupFilter(keywordGroup.index, keywordGroup.type)) {
            			return true;
            		}
            		$.each(keywordGroup.keywords, function(ix2, keyword) {
            			if (settings.keywordFilter(keywordGroup.index, keywordGroup.type, keyword)) {
            				return true;
            			}
            			values.push(keyword.name);
            		});
	            })
	            if ('field' === settings.mode) {
	            	$.each(queryForFields, function(index, fieldName) {
	            		 while ((ix = $.inArray(fieldName, values)) !== -1) {
	            			 values.splice(ix,1);
	                     }
	                })
	            }
	            return $.uniqueSort(values.sort());	            
            },
            mode: 'query',
            queryKeywords : null	
        }, options );

        
        function getCurrentKeywords() {
        	return settings.filter(settings.queryKeywords);
        }
        
        
        function extractAutocompleteTerm(query) {
            if (!query) {
                return { "queryTerm": '', "queryType": "field"};
            } 
            if ('field' === settings.mode) {
            	return { "queryTerm": query, "queryType": "field"};
            }
            var terms = query.match(/AND\sNOT|AND|OR|[^\s"]+|"([^"]*)"/g);
            if (!terms) {
                return { "queryTerm": query, "queryType": "field"};
            }
            if (terms.length == 1) {
                if (isQueryForFieldTerm(terms[0]) && query.endsWith(' ')) {
                    return { "queryTerm": '', "queryType": "fieldTermValue"};
                }
                return { "queryTerm": query, "queryType": "field"};
            } else {
                var lastTerm = terms[terms.length - 1];
                var secondLastTerm = terms[terms.length - 2].trim();
                if (isQueryForFieldTerm(secondLastTerm) && !query.endsWith(' ')) {
                    return { "queryTerm": lastTerm, "queryType": "fieldTermValue"};
                }
                if (isQueryForFieldTerm(lastTerm) && query.endsWith(' ')) {
                    return { "queryTerm": '', "queryType": "fieldTermValue"};
                }
                if (secondLastTerm.endsWith(':')) {
                    return { "queryTerm": null};
                }
                if (queryOperators.indexOf(secondLastTerm) != -1) {
                    return { "queryTerm": lastTerm, "queryType": "field"};
                }
                if (queryOperators.indexOf(lastTerm) != -1 && query.endsWith(' ')) {
                    return { "queryTerm": '', "queryType": "field"};
                }
            }
            return { "queryTerm": null};
        }
        
        function isQueryForFieldTerm(term) {
            if (!term) {
                return false;
            }  
            var termToQuery = term.trim();
            if (endsWith(termToQuery, ':')) {
                termToQuery = termToQuery.substring(0, termToQuery.length - 1);
            }
            return queryForFields.indexOf(termToQuery) != -1;
        }
		
	    return this.each(function() {
	    	$(this).autocomplete({
	            minLength: 0,
	            source: function( request, response ) {
	              var query = extractAutocompleteTerm(request.term);
	              if (query.queryTerm === null) {
	                return;
	              }
	              if ("fieldTermValue" === query.queryType) {
	                var fieldSuggestions = $.grep(getCurrentKeywords(), function( n, i ) {
	                    return queryForFields.indexOf(n) == -1;
	                });
	                response($.ui.autocomplete.filter(fieldSuggestions, query.queryTerm));
	              } else if ("field" === query.queryType) {
	                response($.ui.autocomplete.filter(getCurrentKeywords(), query.queryTerm));
	              }
	            },
	            focus: function() {
	              // prevent value inserted on focus
	              return false;
	            },
	            select: function( event, ui ) {
	              var query = extractAutocompleteTerm(this.value);
	              if ('' === query.queryTerm) {
	                this.value += ui.item.value;
	              } else {
	                var ix = this.value.lastIndexOf(query.queryTerm);
	                if (ix != -1) {
	                    this.value = this.value.substring(0, ix) + ui.item.value;
	                }
	              }
	              if ('query' === settings.mode) {
		              if ("field" === query.queryType) {
		                this.value += ':'
		              }
		              this.value += ' '
	              }
	              $(this).trigger('autocomplete:selected');
	              return false;
	            }
	        })
	    });
	 
	};
	
	function endsWith(value, valueToTest) {
		var d = value.length - valueToTest.length;
		return d >= 0 && value.lastIndexOf(valueToTest) === d;
	}
	
}(jQuery));