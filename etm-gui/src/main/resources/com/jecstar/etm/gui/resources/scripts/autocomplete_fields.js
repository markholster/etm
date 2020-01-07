(function ($) {
	$.fn.autocompleteFieldQuery = function(options) {

        const queryOperators = ['AND NOT', 'AND', 'OR'];
        const joinOperators = ['WITH REQUEST', 'WITH RESPONSE'];
        const queryForFields = ['_exists_'];

        const queryRegexp = new RegExp(queryOperators.join("|").replace(' ', '\\s') + '|[^\\s"]+|"([^"]*)"', 'g');
        const queryAndJoinRegexp = new RegExp(queryOperators.join("|").replace(' ', '\\s') + '|' + joinOperators.join("|").replace(' ', '\\s') + '|[^\\s"]+|"([^"]*)"', 'g');

        const settings = $.extend({
            keywordIndexFilter: function (index) {
                return false;
            },
            keywordGroupFilter: function (index, group) {
                return false;
            },
            keywordFilter: function (index, group, keyword) {
                return false;
            },
            filter: function (keywords) {
                if (keywords == null) {
                    return null;
                }
                const values = [];

                $.each(keywords, function (ix, keywordGroup) {
                    if (settings.keywordIndexFilter(keywordGroup.index)) {
                        return true;
                    }
                    if (settings.keywordGroupFilter(keywordGroup.index, keywordGroup.type)) {
                        return true;
                    }
                    $.each(keywordGroup.keywords, function (ix2, keyword) {
                        if (settings.keywordFilter(keywordGroup.index, keywordGroup.type, keyword)) {
                            return true;
                        }
                        values.push(keyword.name);
                    });
                });
                if ('field' === settings.mode) {
                    $.each(queryForFields, function (index, fieldName) {
                        let ix;
                        while ((ix = $.inArray(fieldName, values)) !== -1) {
                            values.splice(ix, 1);
                        }
                    })
                }
                return $.uniqueSort(values.sort());
            },
            mode: 'query',
            allowJoins: false,
            queryKeywords: null
        }, options);


        function getCurrentKeywords() {
            return settings.filter(settings.queryKeywords);
        }

        function extractAutocompleteTerm(query) {
            if (!query) {
                return {"queryTerm": '', "queryType": "field"};
            }
            if ('field' === settings.mode) {
                return {"queryTerm": query, "queryType": "field"};
            }
            const terms = query.match(settings.allowJoins ? queryAndJoinRegexp : queryRegexp);
            if (!terms) {
                return {"queryTerm": query, "queryType": "field"};
            }
            if (terms.length === 1) {
                if (isQueryForFieldTerm(terms[0]) && endsWith(query, ' ')) {
                    return {"queryTerm": '', "queryType": "fieldTermValue"};
                }
                return {"queryTerm": query, "queryType": "field"};
            } else {
                const lastTerm = terms[terms.length - 1];
                const secondLastTerm = terms[terms.length - 2].trim();
                if (isQueryForFieldTerm(secondLastTerm) && !endsWith(query, ' ')) {
                    return {"queryTerm": lastTerm, "queryType": "fieldTermValue"};
                }
                if (isQueryForFieldTerm(lastTerm) && endsWith(query, ' ')) {
                    return {"queryTerm": '', "queryType": "fieldTermValue"};
                }
                if (endsWith(secondLastTerm, ':')) {
                    return {"queryTerm": null};
                }
                if (queryOperators.indexOf(secondLastTerm) !== -1) {
                    return {"queryTerm": lastTerm, "queryType": "field"};
                }
                if (queryOperators.indexOf(lastTerm) !== -1 && endsWith(query, ' ')) {
                    return {"queryTerm": '', "queryType": "field"};
                }
                if (settings.allowJoins) {
                    let returnValue;
                    const joinIndices = [];
                    $.each(joinOperators, function (ix, operator) {
                        const termIx = terms.indexOf(operator);
                        if (termIx !== -1) {
                            joinIndices.push(termIx);
                        }
                    });
                    joinIndices.sort().reverse();
                    $.each(joinIndices, function (ix, termIx) {
                        if (termIx === terms.length - 2) {
                            returnValue = {"queryTerm": lastTerm, "queryType": "field"};
                        } else if (termIx === terms.length - 1 && endsWith(query, ' ')) {
                            returnValue = {"queryTerm": '', "queryType": "field"};
                        } else {
                            return false;
                        }
                    });

                    if (undefined !== returnValue) {
                        return returnValue;
                    }
                }
            }
            return {"queryTerm": null};
        }
        
        function isQueryForFieldTerm(term) {
            if (!term) {
                return false;
            }
            let termToQuery = term.trim();
            if (endsWith(termToQuery, ':')) {
                termToQuery = termToQuery.substring(0, termToQuery.length - 1);
            }
            return queryForFields.indexOf(termToQuery) !== -1;
        }
		
	    return this.each(function() {
	    	$(this).autocomplete({
	            minLength: 0,
	            source: function( request, response ) {
                    const query = extractAutocompleteTerm(request.term);
	              if (query.queryTerm === null) {
	                return;
	              }
	              if ("fieldTermValue" === query.queryType) {
                      const fieldSuggestions = $.grep(getCurrentKeywords(), function (n, i) {
                          return queryForFields.indexOf(n) === -1;
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
                    const query = extractAutocompleteTerm(this.value);
	              if ('' === query.queryTerm) {
	                this.value += ui.item.value;
	              } else {
                      const ix = this.value.lastIndexOf(query.queryTerm);
                      if (ix !== -1) {
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
        const d = value.length - valueToTest.length;
        return d >= 0 && value.lastIndexOf(valueToTest) === d;
    }
	
}(jQuery));