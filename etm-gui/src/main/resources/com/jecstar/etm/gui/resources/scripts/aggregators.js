const aggregators = (function () {
    'use strict';

    const MAX_SAFE_INTEGER = Number.MAX_SAFE_INTEGER || 9007199254740991;
    const ewmaScript = 'MovingFunctions.ewma(values, params.alpha)';
    const holtScript = 'MovingFunctions.holt(values, params.alpha, params.beta)';
    const holtWinterScript = 'if (values.length > params.bootstrap_window) {MovingFunctions.holtWinters(values, params.alpha, params.beta, params.gamma, params.period, params.multiplicative)}';

    function updatePipelinePaths() {
        $('select[data-element-type=pipeline-path]').each(function (index, select) {
            updatePipelinePathSelectOptions($(select));
        });
    }

    function updateBucketTermOrders() {
        $('select[data-element-type=term-order-by]').each(function (index, select) {
            const $select = $(select);
            const level = $select.closest('.aggregator-container-block').attr('data-aggregator-level');
            updateBucketTermAggregatorsOrderByOptions($select, level);
        });
    }

    function updatePipelinePathSelectOptions($pipelinePathSelect) {
        const currentValue = $pipelinePathSelect.val();
        $pipelinePathSelect.empty();
        const $pipelineAggregatorBlock = $pipelinePathSelect.closest('.pipeline-aggregator-block');
        const $aggregatorContainerBlock = $pipelinePathSelect.closest('.aggregator-container-block');
        const options = getPipelinePathSelectOptions($pipelineAggregatorBlock, $aggregatorContainerBlock);
        $.each(options, function (index, option) {
            $pipelinePathSelect.append($(option));
        });
        commons.sortSelectOptions($pipelinePathSelect);
        if ($pipelinePathSelect.children("option[value='" + currentValue + "']").length) {
            $pipelinePathSelect.val(currentValue);
        }
    }

    function getPipelinePathSelectOptions($pipelineAggregatorBlock, $aggregatorContainerBlock) {
        const result = [];
        const level = Number($aggregatorContainerBlock.attr('data-aggregator-level'));
        // const $pipelineAggregatorBlock = $pipelinePathSelect.closest('.pipeline-aggregator-block');
        const siblingPipeline = isSiblingPipeline($pipelineAggregatorBlock.find("[id^=sel-pipeline-aggregator-]").val());
        // const $aggregatorContainerBlock = $pipelinePathSelect.closest('.aggregator-container-block');
        const $metricAggregators = $aggregatorContainerBlock.find('.metrics-aggregator-block');
        $metricAggregators.each(function (index, metricAggregator) {
            const $metricAggregator = $(metricAggregator);
            const options = getOptionsForMetricAggregator(metricAggregator);
            let $metricContainer = $metricAggregator.closest('.aggregator-container-block');
            let addOption = false;
            if (!siblingPipeline) {
                addOption = true;
            }
            while (Number(level) < Number($metricContainer.attr('data-aggregator-level'))) {
                const $bucketAggregatorContainer = $metricContainer.children('div[data-aggregator-ix="-1"]');
                const parentName = $bucketAggregatorContainer.find('[data-element-type="bucket-aggregator-name"]').val();
                const bucketAggregatorType = $bucketAggregatorContainer.find('[data-element-type="bucket-aggregator-selector"]').val();
                const parentValue = (Number($metricContainer.attr('data-aggregator-level')) - 1) + '-' + $metricContainer.attr('data-aggregator-ix');
                if (isSingleValueBucketAggregator(bucketAggregatorType) && !siblingPipeline) {
                    addOption = true;
                } else if (!isSingleValueBucketAggregator(bucketAggregatorType) && siblingPipeline) {
                    addOption = true;
                } else if (!isSingleValueBucketAggregator(bucketAggregatorType) && !siblingPipeline) {
                    addOption = false;
                    break;
                }
                $.each(options, function (index, option) {
                    option.name = parentName + ' > ' + option.name;
                    option.value = parentValue + '>' + option.value;
                });
                $metricContainer = $metricContainer.parent().closest('.aggregator-container-block');
            }
            if (addOption) {
                $.each(options, function (index, option) {
                    result.push($('<option>').attr('value', option.value).text(option.name));
                });
            }
        });
        const $pipelineAggregators = $aggregatorContainerBlock.find('.pipeline-aggregator-block');
        $pipelineAggregators.each(function (index, pipelineAggregator) {
            const $pipelineAggregator = $(pipelineAggregator);
            if ($pipelineAggregatorBlock.is($pipelineAggregator)) {
                return true;
            }
            let optionName = $pipelineAggregator.find('[id^=input-pipeline-name-]').val();
            let $pipelineContainer = $pipelineAggregator.closest('.aggregator-container-block');
            let optionValue = $pipelineContainer.attr('data-aggregator-level') + '-' + $pipelineAggregator.attr('data-aggregator-ix');
            let addOption = false;
            if (!siblingPipeline) {
                addOption = true;
            }
            while (Number(level) < Number($pipelineContainer.attr('data-aggregator-level'))) {
                const $bucketAggregatorContainer = $pipelineContainer.children('div[data-aggregator-ix="-1"]');
                const parentName = $bucketAggregatorContainer.find('[data-element-type="bucket-aggregator-name"]').val();
                const bucketAggregatorType = $bucketAggregatorContainer.find('[data-element-type="bucket-aggregator-selector"]').val();
                const parentValue = (Number($pipelineContainer.attr('data-aggregator-level')) - 1) + '-' + $pipelineContainer.attr('data-aggregator-ix');
                if (isSingleValueBucketAggregator(bucketAggregatorType) && !siblingPipeline) {
                    addOption = true;
                } else if (!isSingleValueBucketAggregator(bucketAggregatorType) && siblingPipeline) {
                    addOption = true;
                } else if (!isSingleValueBucketAggregator(bucketAggregatorType) && !siblingPipeline) {
                    addOption = false;
                    break;
                }
                optionName = parentName + ' > ' + optionName;
                optionValue = parentValue + '>' + optionValue;
                $pipelineContainer = $pipelineContainer.parent().closest('.aggregator-container-block');
            }
            if (addOption) {
                result.push($('<option>').attr('value', optionValue).text(optionName));
            }
        });
        return result;

        function getOptionsForMetricAggregator(metricAggregator) {
            const options = [];
            const $metricAggregator = $(metricAggregator);
            const dataContext = $metricAggregator.closest('.card').attr('data-context');
            const metricType = $metricAggregator.find('[id^=sel-metrics-aggregator-]').val();
            const $metricContainer = $metricAggregator.closest('.aggregator-container-block');
            const optionName = $metricAggregator.find('[id^=input-metrics-name-]').val();
            const level = $metricContainer.attr('data-aggregator-level');
            const ix = $metricAggregator.attr('data-aggregator-ix');
            const idPrefix = dataContext + '-' + level + '-' + ix;
            let optionValue = level + '-' + ix;
            if ('percentile' === metricType) {
                optionValue += '[' + Number($metricAggregator.find('#input-metrics-percentile-' + idPrefix).val()) + ']';
            } else if ('median' === metricType) {
                optionValue += '[50]';
            } else if ('percentile_rank' === metricType) {
                optionValue += '[' + Number($metricAggregator.find('#input-metrics-percentile-rank-' + idPrefix).val()) + ']';
            } else if ('scripted' === metricType) {
                optionValue += '.value'
            }
            options.push({
                name: optionName,
                value: optionValue
            });
            return options;
        }

        function isSiblingPipeline(pipelineType) {
            return 'average' === pipelineType
                || 'max' === pipelineType
                || 'median' === pipelineType
                || 'min' === pipelineType
                || 'sum' === pipelineType;
        }
    }

    function updatePipelineSelectOptions(bucketAggregatorSelect) {
        const $bucketSelector = $(bucketAggregatorSelect);
        const dataContext = $bucketSelector.closest('div[data-context]').attr('data-context');
        let $container = $bucketSelector.closest('.aggregator-container-block');
        if ('x-axis' === dataContext) {
            $container = $('div[data-context=y-axis]').find('div.aggregator-container-block[data-aggregator-level=0]');
        } else if ('category' === dataContext) {
            $container = $('div[data-context=parts]').find('div.aggregator-container-block[data-aggregator-level=0]');
        }
        $container.children("div.pipeline-aggregator-block").find($('select[data-element-type=pipeline-aggregator-selector]')).each(function (index, select) {
            const $select = $(select);
            const currentValue = $select.val();
            $select.children('option[value="cumulative_sum"], option[value="derivative"], option[value="serial_diff"], option[value="moving_function"]')
                .toggle('date_histogram' === $bucketSelector.val() || 'histogram' === $bucketSelector.val());
            if ($select.children('option[value="' + currentValue + '"]').css('display') === 'none') {
                // Current value now hidden. Reset the selected value to nothing.
                $select.val('').trigger('change');
            }
        });
    }

    function isSingleValueBucketAggregator(aggregatorType) {
        switch (aggregatorType) {
            case 'filter':
            case 'missing':
                return true;
            default:
                return false;
        }
    }

    function updateBucketTermAggregatorsOrderByOptions($orderBySelect, level) {
        const currentValue = $orderBySelect.val();
        $orderBySelect.empty();
        let $aggregatorContainer = null;
        if (level !== undefined) {
            $aggregatorContainer = $orderBySelect.closest('.aggregator-container-block');
        }
        if (level === undefined && $orderBySelect.closest('#card-x-axis').length) {
            level = 0;
            $aggregatorContainer = $('#acc-collapse-y-axis > div.card-body > div.aggregator-container-block');
        }
        if ($aggregatorContainer != null) {
            const $metricAggregators = $aggregatorContainer.find('.metrics-aggregator-block');
            $metricAggregators.each(function (index, metricAggregator) {
                const $metricAggregator = $(metricAggregator);
                let optionName = $metricAggregator.find('[id^=input-metrics-name-]').val();
                let $metricContainer = $metricAggregator.closest('.aggregator-container-block');
                let optionValue = $metricContainer.attr('data-aggregator-level') + '-' + $metricAggregator.attr('data-aggregator-ix');
                while (Number(level) < Number($metricContainer.attr('data-aggregator-level'))) {
                    const bucketAggregatorType = $metricContainer.children('div[data-aggregator-ix="-1"]').find('[data-element-type="bucket-aggregator-selector"]').val();
                    if (bucketAggregatorType !== 'filter' && bucketAggregatorType !== 'missing') {
                        // This is not a single bucket aggregation. We have to skip it as it cannot be used to sort a term.
                        return true;
                    }
                    const parentName = $metricContainer.children('div[data-aggregator-ix="-1"]').find('[data-element-type="bucket-aggregator-name"]').val();
                    const parentValue = (Number($metricContainer.attr('data-aggregator-level')) - 1) + '-' + $metricContainer.attr('data-aggregator-ix');
                    optionName = parentName + ' > ' + optionName;
                    optionValue = parentValue + '>' + optionValue;
                    $metricContainer = $metricContainer.parent().closest('.aggregator-container-block');
                }
                $orderBySelect.append($('<option>').attr('value', optionValue).text(optionName));
            });
        }

        $orderBySelect.append($('<option>').attr('value', '_count').text('Term count'));
        $orderBySelect.append($('<option>').attr('value', '_key').text('Term name'));
        commons.sortSelectOptions($orderBySelect);
        if ($orderBySelect.children("option[value='" + currentValue + "']").length) {
            $orderBySelect.val(currentValue);
        }
    }

    function createPipelineScriptRow(paramName, paramValue, options) {
        const $scriptRow = $('<li class="row">').attr('style', 'margin-top: 5px; list-style-type: none;').append(
            $('<div>').addClass('col-sm-3').append(
                $('<input>')
                    .attr('data-required', 'required')
                    .addClass('form-control form-control-sm')
                    .attr('placeholder', 'param_name...')
            ),
            $('<div>').addClass('input-group col-sm-9').append(
                $('<select>')
                    .attr('data-required', 'required')
                    .attr('data-element-type', 'pipeline-path')
                    .addClass('form-control form-control-sm custom-select custom-select-sm'),
                $('<div>').addClass('input-group-append').append(
                    $('<button>').addClass('btn btn-outline-secondary fa fa-times text-danger').attr('type', 'button').on('click', function (event) {
                        event.preventDefault();
                        $(this).closest('li').remove();
                    })
                )
            )
        );
        const $select = $scriptRow.find('select');
        if (options) {
            $.each(options, function (index, option) {
                $select.append($(option));
            });
        }
        if (paramName) {
            $scriptRow.find('input').val(paramName);
        }
        if (paramValue) {
            $select.val(paramValue);
        }
        return $scriptRow;
    }

    function findFieldType(indexName, fieldName) {
        let fieldType = null;
        $.each(aggregators.config.keywords, function (index, keywordGroup) {
            if (indexName === keywordGroup.index) {
                const result = $.grep(keywordGroup.keywords, function (e) {
                    return e.name === fieldName;
                });
                if (result.length >= 1) {
                    fieldType = result[0].type;
                    return true;
                }
            }
        });
        return fieldType;
    }

    const initialize = function (settings) {
        aggregators.config = {
            page: $('body > .container-fluid'),
            enableOrDisableButtons: function () {
            },
            keywords: []
        };
        $.extend(aggregators.config, settings);

        // Handle the addition of a metrics aggregator on y-axis.
        aggregators.config.page.on('click', "a[data-element-type='add-metrics-aggregator']", function (event) {
            event.preventDefault();
            let highestIx = -1;
            const dataContext = $(this).closest('div[data-context]').attr('data-context');
            const $metricContainer = $(this).closest('.aggregator-container-block');
            $metricContainer.children().each(function () {
                const currentIx = Number($(this).attr('data-aggregator-ix'));
                if (currentIx !== undefined && currentIx > highestIx) {
                    highestIx = currentIx;
                }
            });
            addMetricsAggregator(dataContext, $metricContainer, Number($metricContainer.attr('data-aggregator-level')), highestIx + 1);
            updateBucketTermOrders();
            updatePipelinePaths();
            aggregators.config.enableOrDisableButtons();
        });

        // Add the metric aggregator change handling.
        aggregators.config.page.on('change', "select[data-element-type='metrics-aggregator-selector']", function (event) {
            event.preventDefault();
            const $select = $(this);
            const $val = $select.val();
            const $metricContainer = $select.closest('.metrics-aggregator-block');

            $metricContainer.find('[id^=input-metrics-field-]').parent().parent().toggle(('count' !== $val && 'scripted' !== $val));
            $metricContainer.find('[id^=input-metrics-percentile-]').parent().parent().toggle('percentile' === $val);
            $metricContainer.find('[id^=input-metrics-percentile-rank-]').parent().parent().toggle('percentile_rank' === $val);
            $metricContainer.find('[id^=input-metrics-scripted-init-]').parent().parent().toggle('scripted' === $val);
            $metricContainer.find('[id^=input-metrics-scripted-map-]').parent().parent().toggle('scripted' === $val);
            $metricContainer.find('[id^=input-metrics-scripted-combine-]').parent().parent().toggle('scripted' === $val);
            $metricContainer.find('[id^=input-metrics-scripted-reduce-]').parent().parent().toggle('scripted' === $val);
            updateBucketTermOrders();
            updatePipelinePaths();
        });

        // Add the remove metrics aggregator handling.
        aggregators.config.page.on('click', "a[data-element-type='remove-metrics-aggregator']", function (event) {
            event.preventDefault();
            $(this).closest('.metrics-aggregator-block').remove();
            // Remove the option form the term order by select.
            updateBucketTermOrders();
            updatePipelinePaths();
            aggregators.config.enableOrDisableButtons();
        });

        // Handle the addition of a pipeline aggregator on y-axis.
        aggregators.config.page.on('click', "a[data-element-type='add-pipeline-aggregator']", function (event) {
            event.preventDefault();
            let highestIx = -1;
            const dataContext = $(this).closest('div[data-context]').attr('data-context');
            const $pipelineContainer = $(this).closest('.aggregator-container-block');
            $pipelineContainer.children().each(function () {
                const currentIx = Number($(this).attr('data-aggregator-ix'));
                if (currentIx !== undefined && currentIx > highestIx) {
                    highestIx = currentIx;
                }
            });
            addPipelineAggregator(dataContext, $pipelineContainer, Number($pipelineContainer.attr('data-aggregator-level')), highestIx + 1);
            // Find the corresponding bucket aggregator this pipeline belongs to because depending on the bucket type some pipeline aggregator types should not be visible
            if ('0' === $pipelineContainer.attr('data-aggregator-level')) {
                // Root level, we have to pick the bucket aggregator from another section.
                if ('y-axis' === dataContext) {
                    $('#sel-bucket-aggregator-x-axis-0-0').trigger('change');
                } else if ('parts' === dataContext) {
                    $('#sel-bucket-aggregator-category-0-0').trigger('change');
                }
            } else {
                $('#sel-bucket-aggregator-' + dataContext + '-' + (Number($pipelineContainer.attr('data-aggregator-level')) - 1) + '-' + $pipelineContainer.attr('data-aggregator-ix')).trigger('change');
            }
            aggregators.config.enableOrDisableButtons();
        });

        // Handle the name change of a pipeline aggregator selection
        aggregators.config.page.on('change', "select[data-element-type='pipeline-aggregator-selector']", function (event) {
            event.preventDefault();
            const $select = $(this);
            const $container = $select.closest('.pipeline-aggregator-block');
            const $val = $select.val();
            $container.find('[id^=grp-pipeline-path-]').toggle(('scripted' !== $val));
            $container.find('[id^=grp-pipeline-script-params-], [id^=grp-pipeline-script-]').toggle(('scripted' === $val));
            $container.find('[id^=grp-pipeline-lag-]').toggle(('serial_diff' === $val));
            $container.find('[id^=grp-pipeline-window-]').toggle(('moving_function' === $val));
            $container.find('[id^=grp-pipeline-function-]').toggle(('moving_function' === $val));
            if ('moving_function' === $val) {
                $container.find('[id^=sel-pipeline-function-]').trigger('change');
            } else {
                $container.find('[id^=grp-pipeline-alpha-], [id^=grp-pipeline-beta-], [id^=grp-pipeline-gamma-], [id^=grp-pipeline-period-], [id^=grp-pipeline-multiplicative-]').hide();
            }
            updatePipelinePaths();
        });

        // Add the remove pipeline aggregator handling.
        aggregators.config.page.on('click', "a[data-element-type='remove-pipeline-aggregator']", function (event) {
            event.preventDefault();
            $(this).closest('.pipeline-aggregator-block').remove();
            updatePipelinePaths();
            aggregators.config.enableOrDisableButtons();
        });

        // Add the add pipeline script parameter handler.
        aggregators.config.page.on('click', "a[data-element-type='add-pipeline-script-param']", function (event) {
            event.preventDefault();
            $(this).closest('.pipeline-aggregator-block').find('ol[id^=list-pipeline-script-params-]').append(
                createPipelineScriptRow()
            );
            updatePipelinePaths();
            aggregators.config.enableOrDisableButtons();
        });

        // Handle the addition of a bucket aggregator.
        aggregators.config.page.on('click', "a[data-element-type='add-bucket-aggregator']", function (event) {
            event.preventDefault();
            let highestIx = -1;
            const dataContext = $(this).closest('div[data-context]').attr('data-context');
            const $bucketContainer = $(this).closest('.aggregator-container-block');
            $bucketContainer.children().each(function () {
                let currentIx = Number($(this).attr('data-aggregator-ix'));
                if (currentIx !== undefined && currentIx > highestIx) {
                    highestIx = currentIx;
                }
            });
            addBucketAggregator(dataContext, $bucketContainer, Number($bucketContainer.attr('data-aggregator-level')) + 1, highestIx + 1);
            aggregators.config.enableOrDisableButtons();
        });

        // Handle the name change of a bucket aggregator selection
        aggregators.config.page.on('change', "select[data-element-type='bucket-aggregator-selector']", function (event) {
            event.preventDefault();
            const $container = $(this).closest('.bucket-aggregator-block');
            const val = $(this).val();
            $container.find('[id^=grp-bucket-date-interval]').toggle(('date_histogram' === val));
            $container.find('[id^=grp-bucket-histogram-interval]').toggle(('histogram' === val));
            $container.find('[id^=grp-bucket-term-order-by]').toggle(('term' === val));
            $container.find('[id^=grp-bucket-term-order]').toggle(('term' === val));
            $container.find('[id^=grp-bucket-min-doc-count]').toggle(('date_histogram' === val || 'histogram' === val || 'term' === val || 'significant_term' === val));
            $container.find('[id^=grp-bucket-term-top]').toggle(('term' === val || 'significant_term' === val));
            $container.find('[id^=grp-bucket-filter-value]').toggle(('filter' === val));

            const $rootContainer = $container.closest('div.aggregator-container-block[data-aggregator-level="0"]');
            if ('true' === $rootContainer.attr('data-require-single-value')) {
                if (isSingleValueBucketAggregator(val)) {
                    $container.find("select[data-element-type='show-on-graph-selector']").removeAttr('disabled').trigger('change');
                } else {
                    $container.find("select[data-element-type='show-on-graph-selector']").attr('disabled', 'disabled').trigger('change');
                }
            }
            updateBucketTermOrders();
            updatePipelinePaths();
            updatePipelineSelectOptions($(this));
        });

        // Add the remove bucket aggregator handling.
        aggregators.config.page.on('click', "a[data-element-type='remove-bucket-aggregator']", function (event) {
            event.preventDefault();
            $(this).closest('.aggregator-container-block').remove();
            updateBucketTermOrders();
            updatePipelinePaths();
            aggregators.config.enableOrDisableButtons();
        });

        // Handle the name change of a metric name on the y-axis
        aggregators.config.page.on('input', "input[data-element-type='metric-aggregator-name']", function (event) {
            event.preventDefault();
            updateBucketTermOrders();
            updatePipelinePaths();
        });

        // Handle the name change of a bucket name on the y-axis
        aggregators.config.page.on('input', "input[data-element-type='bucket-aggregator-name']", function (event) {
            event.preventDefault();
            updateBucketTermOrders();
            updatePipelinePaths();
        });

        // Handle the enable or disable state for a change on the show on graph selectors
        aggregators.config.page.on('change', "select[id^='sel-bucket-show-on-graph-']", function (event) {
            event.preventDefault();
            const $this = $(this);
            const $container = $this.closest('.aggregator-container-block');
            const childrenEnabled = $this.val() === 'true';
            if (!childrenEnabled) {
                // Disable all show-on-graph-selectors
                $container.find("select[data-element-type='show-on-graph-selector']").each(function (index, element) {
                    const $element = $(element);
                    if ($element.is($this)) {
                        return true;
                    }
                    $element.attr('disabled', 'disabled');
                });
            } else {
                // Enable all direct metrics and pipeline aggregators
                $container.find("> div.pipeline-aggregator-block > div > div > select[data-element-type='show-on-graph-selector'], > div.metrics-aggregator-block > div > div > select[data-element-type='show-on-graph-selector']").each(function (index, element) {
                    const $element = $(element);
                    if ($element.attr('disabled') !== undefined) {
                        $element.removeAttr('disabled');
                    }
                });
                // Enable through the bucket hierarchy until the bucket has a show-on-graph-selectors with value false
                $container.find("> div.aggregator-container-block > div.bucket-aggregator-block > div > div > select[data-element-type='show-on-graph-selector']").each(function (index, element) {
                    const $element = $(element);
                    if ($element.attr('disabled') !== undefined) {
                        $element.removeAttr('disabled');
                        if ($element.val() === 'true') {
                            // Element is visible on graph. Continue with enabling on hierarchy.
                            $element.trigger('change');
                        }
                    }
                });
            }
        });

        aggregators.config.page.on('change', "select[data-element-type='pipeline-function-selector']", function (event) {
            event.preventDefault();
            const $select = $(this);
            const $val = $select.val();
            const $pipelineContainer = $select.closest('.pipeline-aggregator-block');
            if (ewmaScript === $val) {
                $pipelineContainer.find('[id^=grp-pipeline-alpha-]').show()
                    .find('label').text('Exponential decay');
                $pipelineContainer.find('[id^=grp-pipeline-beta-], [id^=grp-pipeline-gamma-], [id^=grp-pipeline-period-], [id^=grp-pipeline-multiplicative-]').hide();
                $pipelineContainer.find('[id^=input-pipeline-window-]').attr('min', '1');
            } else if (holtScript === $val) {
                $pipelineContainer.find('[id^=grp-pipeline-alpha-]').show()
                    .find('label').text('Level decay');
                $pipelineContainer.find('[id^=grp-pipeline-beta-]').show()
                    .find('label').text('Trend decay');
                $pipelineContainer.find('[id^=grp-pipeline-gamma-], [id^=grp-pipeline-period-], [id^=grp-pipeline-multiplicative-]').hide();
                $pipelineContainer.find('[id^=input-pipeline-window-]').attr('min', '1');
            } else if (holtWinterScript === $val) {
                $pipelineContainer.find('[id^=grp-pipeline-alpha-]').show()
                    .find('label').text('Level decay');
                $pipelineContainer.find('[id^=grp-pipeline-beta-]').show()
                    .find('label').text('Trend decay');
                $pipelineContainer.find('[id^=grp-pipeline-gamma-]').show()
                    .find('label').text('Seasonality decay');
                $pipelineContainer.find('[id^=grp-pipeline-period-], [id^=grp-pipeline-multiplicative-]').show();
                $pipelineContainer.find('[data-element-type=pipeline-periodicity]').trigger('change');
            } else {
                $pipelineContainer.find('[id^=grp-pipeline-alpha-], [id^=grp-pipeline-beta-], [id^=grp-pipeline-gamma-], [id^=grp-pipeline-period-], [id^=grp-pipeline-multiplicative-]').hide();
                $pipelineContainer.find('[id^=input-pipeline-window-]').attr('min', '1');
            }
        });

        aggregators.config.page.on('change', "input[data-element-type='pipeline-periodicity']", function () {
            const $input = $(this);
            const period = Number($input.val());
            const $pipelineContainer = $input.closest('.pipeline-aggregator-block');
            $pipelineContainer.find('[id^=input-pipeline-window-]').attr('min', period * 2);
            aggregators.config.enableOrDisableButtons();
        });

    };

    const addMetricsAggregator = function addMetricsAggregatorBlock(dataContext, parent, level, ix, aggregatorData) {
        const idSuffix = dataContext + '-' + level + '-' + ix;
        const $container = $('<div class="metrics-aggregator-block" data-aggregator-ix="' + ix + '">');
        const $parent = $(parent);
        if (ix > 0 || (ix === 0 && level > 0)) {
            $container.append(
                $('<hr>').attr('style', 'border-top: dashed 1px;')
            );
        }
        const defaultData = {
            name: 'Metric ' + level + '-' + ix,
            show_on_graph: 'true',
            metrics_type: 'average',
            field: '',
            percentile: 95,
            rank: null,
            init_script: null,
            map_script: null,
            combine_script: null,
            reduce_script: null
        };
        if (!aggregatorData) {
            aggregatorData = defaultData;
        }
        $container.append(
            $('<div>').addClass('form-group row').append(
                $('<label>').attr('for', 'input-metrics-name-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Name').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-name-' + idSuffix)
                        .attr('type', 'text')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'metric-aggregator-name')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.name ? aggregatorData.name : defaultData.name)
                )
            ),
            $('<div>').addClass('form-group row').append(
                $('<label>').attr('for', 'sel-metrics-show-on-graph-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Show on graph').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<select>').attr('id', 'sel-metrics-show-on-graph-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'show-on-graph-selector')
                        .addClass('form-control form-control-sm custom-select custom-select-sm')
                        .append(
                            $('<option>').attr('value', 'true').text('Yes'),
                            $('<option>').attr('value', 'false').text('No')
                        )
                        .val(function () {
                            if (aggregatorData.show_on_graph !== undefined) {
                                return aggregatorData.show_on_graph ? 'true' : 'false';
                            }
                            return defaultData.show_on_graph;
                        })
                )
            ),
            $('<div>').addClass('form-group row').append(
                $('<label>').attr('for', 'sel-metrics-aggregator-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Aggregator').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<select>').attr('id', 'sel-metrics-aggregator-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'metrics-aggregator-selector')
                        .addClass('form-control form-control-sm custom-select custom-select-sm')
                        .append(
                            $('<option>').attr('value', 'average').text('Average'),
                            $('<option>').attr('value', 'count').text('Count'),
                            $('<option>').attr('value', 'max').text('Max'),
                            $('<option>').attr('value', 'median').text('Median'),
                            $('<option>').attr('value', 'median_absolute_deviation').text('Median absolute deviation'),
                            $('<option>').attr('value', 'min').text('Min'),
                            $('<option>').attr('value', 'percentile').text('Percentile'),
                            $('<option>').attr('value', 'percentile_rank').text('Percentile rank'),
                            $('<option>').attr('value', 'scripted').text('Scripted'),
                            $('<option>').attr('value', 'sum').text('Sum'),
                            $('<option>').attr('value', 'cardinality').text('Unique count')
                        )
                        .val(aggregatorData.metrics_type ? aggregatorData.metrics_type : defaultData.metrics_type)
                )
            ),
            $('<div>').addClass('form-group row').append(
                $('<label>').attr('for', 'input-metrics-field-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Field').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-field-' + idSuffix).attr('type', 'text')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'autocomplete-input')
                        .addClass('form-control form-control-sm')
                        .autocompleteFieldQuery(
                            {
                                queryKeywords: aggregators.config.keywords,
                                mode: 'field',
                                keywordIndexFilter: function (index) {
                                    return index !== $('#sel-data-source').val();
                                },
                                keywordFilter: function (index, group, keyword) {
                                    const agg = $('#sel-metrics-aggregator-' + idSuffix).val();
                                    if ('average' === agg || 'sum' === agg || 'median' === agg || 'median_absolute_deviation' === agg || 'percentile' === agg || 'percentile_rank' === agg) {
                                        return !keyword.number;
                                    } else if ('min' === agg || 'max' === agg) {
                                        return !keyword.number && !keyword.date;
                                    } else if ('cardinality' === agg) {
                                        return !keyword.number && !keyword.date && 'text' !== keyword.type;
                                    } else {
                                        return true;
                                    }
                                }
                            }
                        )
                        .val(aggregatorData.field ? aggregatorData.field : defaultData.field)
                )
            ),
            $('<div>').addClass('form-group row').attr('style', 'display: none;').append(
                $('<label>').attr('for', 'input-metrics-percentile-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Percentile').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-percentile-' + idSuffix)
                        .attr('type', 'number')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .attr('step', 'any')
                        .attr('min', '0')
                        .attr('max', '100')
                        .val(aggregatorData.percentile ? aggregatorData.percentile : defaultData.percentile)
                )
            ),
            $('<div>').addClass('form-group row').attr('style', 'display: none;').append(
                $('<label>').attr('for', 'input-metrics-percentile-rank-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Rank').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-percentile-rank-' + idSuffix)
                        .attr('type', 'number')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .attr('step', 'any')
                        .val(aggregatorData.rank ? aggregatorData.rank : defaultData.rank)
                )
            ),
            $('<div>').addClass('form-group row').attr('style', 'display: none;').append(
                $('<label>').attr('for', 'input-metrics-scripted-init-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Init script'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-scripted-init-' + idSuffix)
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.init_script ? aggregatorData.init_script : defaultData.init_script)
                )
            ),
            $('<div>').addClass('form-group row').attr('style', 'display: none;').append(
                $('<label>').attr('for', 'input-metrics-scripted-map-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Map script').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-scripted-map-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.map_script ? aggregatorData.map_script : defaultData.map_script)
                )
            ),
            $('<div>').addClass('form-group row').attr('style', 'display: none;').append(
                $('<label>').attr('for', 'input-metrics-scripted-combine-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Combine script'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-scripted-combine-' + idSuffix)
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.combine_script ? aggregatorData.combine_script : defaultData.combine_script)
                )
            ),
            $('<div>').addClass('form-group row').attr('style', 'display: none;').append(
                $('<label>').attr('for', 'input-metrics-scripted-reduce-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Reduce script'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-metrics-scripted-reduce-' + idSuffix)
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.reduce_script ? aggregatorData.reduce_script : defaultData.reduce_script)
                )
            )
        );
        $container.append(
            $('<a>').attr('href', '#').addClass('float-right')
                .attr('data-element-type', 'remove-metrics-aggregator')
                .text('Remove metric'),
            $('<br>')
        );
        if (level > 0) {
            const $bucketShowOnGraph = $parent.find("> div[data-aggregator-ix='-1'] > div > div > select[data-element-type='show-on-graph-selector']");
            if ($bucketShowOnGraph.val() === 'false' || $bucketShowOnGraph.attr('disabled') !== undefined) {
                $container.find("select[data-element-type='show-on-graph-selector']").attr('disabled', 'disabled');
            }
        }
        $parent.append($container);
    };

    const addBucketAggregator = function addBucketAggregatorBlock(dataContext, parent, level, ix, aggregatorData) {
        const idSuffix = dataContext + '-' + (level - 1) + '-' + ix;
        const $parent = $(parent);
        const $container = $('<div class="aggregator-container-block border rounded bg" data-aggregator-ix="' + ix + '" data-aggregator-level="' + level + '" style="margin: 0 0 0.5rem 0.25rem; padding: 0.5rem;">');
        if (level % 2 === 1) {
            $container.addClass('bg-light');
        } else {
            $container.addClass('bg-white');
        }
        $parent.append($container);
        const defaultData = {
            name: 'Bucket ' + (level - 1) + '-' + ix,
            show_on_graph: 'true',
            bucket_type: 'date_histogram',
            field: null,
            interval: 'auto',
            top: 5,
            order_by: '_count',
            order: 'asc',
            min_doc_count: null,
            value: null

        };
        if (!aggregatorData) {
            aggregatorData = defaultData;
        }
        $container.append(
            $('<div class="bucket-aggregator-block" data-aggregator-ix="-1">').append(
                $('<div>').addClass('form-group row').append(
                    $('<label>').attr('for', 'input-bucket-name-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Name').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<input>').attr('id', 'input-bucket-name-' + idSuffix)
                            .attr('type', 'text')
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .attr('data-element-type', 'bucket-aggregator-name')
                            .addClass('form-control form-control-sm')
                            .val(aggregatorData.name ? aggregatorData.name : defaultData.name)
                    )
                ),
                $('<div>').addClass('form-group row').append(
                    $('<label>').attr('for', 'sel-bucket-show-on-graph-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Show on graph').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<select>').attr('id', 'sel-bucket-show-on-graph-' + idSuffix)
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .attr('data-element-type', 'show-on-graph-selector')
                            .addClass('form-control form-control-sm custom-select custom-select-sm')
                            .append(
                                $('<option>').attr('value', 'true').text('Yes'),
                                $('<option>').attr('value', 'false').text('No')
                            )
                            .val(function () {
                                if (aggregatorData.show_on_graph !== undefined) {
                                    return aggregatorData.show_on_graph ? 'true' : 'false';
                                }
                                return defaultData.show_on_graph;
                            })
                    )
                ),
                $('<div>').addClass('form-group row').append(
                    $('<label>').attr('for', 'sel-bucket-aggregator-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Aggregator').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<select>').attr('id', 'sel-bucket-aggregator-' + idSuffix)
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .attr('data-element-type', 'bucket-aggregator-selector')
                            .addClass('form-control form-control-sm custom-select custom-select-sm')
                            .append(
                                $('<option>').attr('value', 'date_histogram').text('Date histogram'),
                                $('<option>').attr('value', 'filter').text('Filter'),
                                $('<option>').attr('value', 'histogram').text('Histogram'),
                                $('<option>').attr('value', 'missing').text('Missing'),
                                $('<option>').attr('value', 'significant_term').text('Significant term'),
                                $('<option>').attr('value', 'term').text('Term')
                            )
                            .val(aggregatorData.bucket_type ? aggregatorData.bucket_type : defaultData.bucket_type)
                    )
                ),
                $('<div>').addClass('form-group row').append(
                    $('<label>').attr('for', 'input-bucket-field-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Field').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<input>').attr('id', 'input-bucket-field-' + idSuffix).attr('type', 'text')
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .attr('data-element-type', 'autocomplete-input')
                            .addClass('form-control form-control-sm')
                            .autocompleteFieldQuery(
                                {
                                    queryKeywords: aggregators.config.keywords,
                                    mode: 'field',
                                    keywordIndexFilter: function (index) {
                                        return index !== $('#sel-data-source').val();
                                    },
                                    keywordFilter: function (index, group, keyword) {
                                        const agg = $('#sel-bucket-aggregator-' + idSuffix).val();
                                        if ('date_histogram' === agg || 'date_range' === agg) {
                                            return !keyword.date;
                                        } else if ('histogram' === agg || 'range' === agg) {
                                            return !keyword.number;
                                        } else if ('significant_term' === agg) {
                                            return 'text' !== keyword.type;
                                        } else if ('term' === agg || 'filter' === agg) {
                                            return !keyword.number && !keyword.date && 'text' !== keyword.type;
                                        } else {
                                            return false;
                                        }
                                    }
                                }
                            )
                            .val(aggregatorData.field ? aggregatorData.field : defaultData.field)
                    )
                ),
                $('<div>').addClass('form-group row').attr('id', 'grp-bucket-date-interval-' + idSuffix).append(
                    $('<label>').attr('for', 'sel-bucket-date-interval-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Interval').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<select>').attr('id', 'sel-bucket-date-interval-' + idSuffix)
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .attr('data-element-type', 'bucket-date-interval-selector')
                            .addClass('form-control form-control-sm custom-select custom-select-sm')
                            .append(
                                $('<option>').attr('value', 'auto').text('Auto'),
                                $('<option>').attr('value', 'seconds').text('Seconds'),
                                $('<option>').attr('value', 'minutes').text('Minutes'),
                                $('<option>').attr('value', 'hours').text('Hours'),
                                $('<option>').attr('value', 'days').text('Days'),
                                $('<option>').attr('value', 'weeks').text('Weeks'),
                                $('<option>').attr('value', 'months').text('Months'),
                                $('<option>').attr('value', 'quarters').text('Quarters'),
                                $('<option>').attr('value', 'years').text('Years')
                            )
                            .val(function () {
                                if (aggregatorData.interval && !commons.isNumeric(aggregatorData.interval)) {
                                    return aggregatorData.interval;
                                }
                                return defaultData.interval;
                            })
                    )
                ),
                $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-bucket-histogram-interval-' + idSuffix).append(
                    $('<label>').attr('for', 'input-bucket-histogram-interval-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Interval').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<div>').addClass('input-group input-group-sm').append(
                            $('<input>').attr('id', 'input-bucket-histogram-interval-' + idSuffix)
                                .attr('type', 'number')
                                .attr('required', 'required')
                                .attr('data-required', 'required')
                                .attr('step', 'any')
                                .attr('min', '0.1')
                                .addClass('form-control form-control-sm')
                                .val(function () {
                                    if (aggregatorData.interval && commons.isNumeric(aggregatorData.interval)) {
                                        return aggregatorData.interval;
                                    }
                                    return '';
                                })
                        )
                    )
                ),
                $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-bucket-term-top-' + idSuffix).append(
                    $('<label>').attr('for', 'input-bucket-term-top-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Top').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<div>').addClass('input-group input-group-sm').append(
                            $('<input>').attr('id', 'input-bucket-term-top-' + idSuffix)
                                .attr('type', 'number')
                                .attr('required', 'required')
                                .attr('data-required', 'required')
                                .attr('min', '1')
                                .addClass('form-control form-control-sm')
                                .val(aggregatorData.top ? aggregatorData.top : defaultData.top)
                        )
                    )
                ),
                $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-bucket-term-order-by-' + idSuffix).append(
                    $('<label>').attr('for', 'sel-bucket-term-order-by-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Order by').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<select>').attr('id', 'sel-bucket-term-order-by-' + idSuffix)
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .attr('data-element-type', 'term-order-by')
                            .addClass('form-control form-control-sm custom-select custom-select-sm')
                            .append(
                                $('<option>').attr('value', '_count').text('Term count'),
                                $('<option>').attr('value', '_key').text('Term name')
                            )
                            .val(aggregatorData.order_by ? aggregatorData.order_by : defaultData.order_by)
                    )
                ),
                $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-bucket-term-order-' + idSuffix).append(
                    $('<label>').attr('for', 'sel-bucket-term-order-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Order').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<select>').attr('id', 'sel-bucket-term-order-' + idSuffix)
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .addClass('form-control form-control-sm custom-select custom-select-sm')
                            .append(
                                $('<option>').attr('value', 'asc').text('Ascending'),
                                $('<option>').attr('value', 'desc').text('Descending')
                            )
                            .val(aggregatorData.order ? aggregatorData.order : defaultData.order)
                    )
                ),
                $('<div>').addClass('form-group row').attr('id', 'grp-bucket-min-doc-count-' + idSuffix).append(
                    $('<label>').attr('for', 'input-bucket-min-doc-count-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Min nr of events'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<div>').addClass('input-group input-group-sm').append(
                            $('<input>').attr('id', 'input-bucket-min-doc-count-' + idSuffix)
                                .attr('type', 'number')
                                .attr('min', '0')
                                .addClass('form-control form-control-sm')
                                .val(aggregatorData.min_doc_count !== undefined ? aggregatorData.min_doc_count : defaultData.min_doc_count)
                        )
                    )
                ),
                $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-bucket-filter-value-' + idSuffix).append(
                    $('<label>').attr('for', 'input-bucket-filter-value-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Value').css('font-style', 'italic'),
                    $('<div>').addClass('col-sm-9').append(
                        $('<div>').addClass('input-group input-group-sm').append(
                            $('<input>').attr('id', 'input-bucket-filter-value-' + idSuffix)
                                .attr('type', 'text')
                                .attr('required', 'required')
                                .attr('data-required', 'required')
                                .addClass('form-control form-control-sm')
                                .val(aggregatorData.value ? aggregatorData.value : defaultData.value)
                        )
                    )
                )
            )
        );
        $container.append(
            $('<div class="d-flex justify-content-end bucket-aggregator-header">').append(
                $('<div class="p-1">').append($('<a href="#" data-element-type="add-bucket-aggregator">Add bucket</a>')),
                $('<div class="p-1">').append($('<a href="#" data-element-type="add-metrics-aggregator">Add metric</a>')),
                $('<div class="p-1">').append($('<a href="#" data-element-type="add-pipeline-aggregator">Add pipeline</a>')),
                $('<div class="p-1">').append($('<a href="#" data-element-type="remove-bucket-aggregator">Remove bucket</a>'))
            )
        );
        if ('parts' === dataContext) {
            // For parts only a single level is allowed. So this bucket might not been shown on the graph. To trigger the potential disabling of the select box we have to fire this event over here.
            $container.find("select[data-element-type='bucket-aggregator-selector']").trigger('change');
        }
        if (level > 0) {
            const $bucketShowOnGraph = $parent.find("> div[data-aggregator-ix='-1'] > div > div > select[data-element-type='show-on-graph-selector']");
            if ($bucketShowOnGraph.val() === 'false' || $bucketShowOnGraph.attr('disabled') !== undefined) {
                $container.find("select[data-element-type='show-on-graph-selector']").attr('disabled', 'disabled');
            }
        }
        const $orderBySelector = $container.find('select[data-element-type=term-order-by]');
        const orderByValue = aggregatorData.order_by ? aggregatorData.order_by : defaultData.order_by;
        if (orderByValue && orderByValue !== $orderBySelector.val()) {
            // The value that should be set could not be set on the select element because the correct option isn't available yet. We add a dummy option here so the updateBucketTermOrders method can set the correct value later on.
            $orderBySelector.append(
                $('<option>').attr('value', orderByValue).text('Unknown')
            );
            $orderBySelector.val(orderByValue);
        }

        if (aggregatorData.aggregators) {
            $.each(aggregatorData.aggregators, function (ix, aggregator) {
                if ('metrics' === aggregator.type) {
                    addMetricsAggregator(dataContext, $container, level, ix, aggregator);
                } else if ('bucket' === aggregator.type) {
                    addBucketAggregator(dataContext, $container, level + 1, ix, aggregator);
                } else if ('pipeline' === aggregator.type) {
                    addPipelineAggregator(dataContext, $container, level, ix, aggregator);
                }
            });
        }

    };

    const addPipelineAggregator = function addPipelineAggregatorBlock(dataContext, parent, level, ix, aggregatorData) {
        const idSuffix = dataContext + '-' + level + '-' + ix;
        const $container = $('<div class="pipeline-aggregator-block" data-aggregator-ix="' + ix + '">');
        const $parent = $(parent);
        if (ix > 0 || (ix >= 0 && level > 0)) {
            $container.append(
                $('<hr>').attr('style', 'border-top: dashed 1px;')
            );
        }
        const defaultData = {
            name: 'Pipeline ' + level + '-' + ix,
            show_on_graph: 'true',
            pipeline_type: 'average',
            script_params: [],
            script: null,
            path: null,
            lag: 1.,
            window: 1,
            alpha: 0.3,
            beta: 0.1,
            gamma: 0.1,
            period: 5,
            multiplicative: 'false'

        };
        if (!aggregatorData) {
            aggregatorData = defaultData;
        }
        $container.append(
            $('<div>').addClass('form-group row').append(
                $('<label>').attr('for', 'input-pipeline-name-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Name').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-pipeline-name-' + idSuffix)
                        .attr('type', 'text')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'pipeline-aggregator-name')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.name ? aggregatorData.name : defaultData.name)
                )
            ),
            $('<div>').addClass('form-group row').append(
                $('<label>').attr('for', 'sel-pipeline-show-on-graph-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Show on graph').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<select>').attr('id', 'sel-pipeline-show-on-graph-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'show-on-graph-selector')
                        .addClass('form-control form-control-sm custom-select custom-select-sm')
                        .append(
                            $('<option>').attr('value', 'true').text('Yes'),
                            $('<option>').attr('value', 'false').text('No')
                        ).val(function () {
                        if (aggregatorData.show_on_graph !== undefined) {
                            return aggregatorData.show_on_graph ? 'true' : 'false';
                        }
                        return defaultData.show_on_graph;
                    })
                )
            ),
            $('<div>').addClass('form-group row').append(
                $('<label>').attr('for', 'sel-pipeline-aggregator-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Aggregator').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<select>').attr('id', 'sel-pipeline-aggregator-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'pipeline-aggregator-selector')
                        .addClass('form-control form-control-sm custom-select custom-select-sm')
                        .append(
                            $('<option>').attr('value', 'average').text('Average'),
                            $('<option>').attr('value', 'cumulative_sum').text('Cumulative sum'),
                            $('<option>').attr('value', 'derivative').text('Derivative'),
                            $('<option>').attr('value', 'max').text('Max'),
                            $('<option>').attr('value', 'median').text('Median'),
                            $('<option>').attr('value', 'min').text('Min'),
                            $('<option>').attr('value', 'moving_function').text('Moving function'),
                            $('<option>').attr('value', 'scripted').text('Scripted'),
                            $('<option>').attr('value', 'serial_diff').text('Serial differencing'),
                            $('<option>').attr('value', 'sum').text('Sum')
                        )
                        .val(aggregatorData.pipeline_type ? aggregatorData.pipeline_type : defaultData.pipeline_type)
                )
            ),
            $('<div>').addClass('form-group row').attr('id', 'grp-pipeline-path-' + idSuffix).append(
                $('<label>').attr('for', 'sel-pipeline-path-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Path').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<select>').attr('id', 'sel-pipeline-path-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'pipeline-path')
                        .addClass('form-control form-control-sm custom-select custom-select-sm')
                        .val(aggregatorData.path ? aggregatorData.path : defaultData.path)
                )
            ),
            $('<div style="display: none">').addClass('form-group').attr('id', 'grp-pipeline-script-params-' + idSuffix).append(
                $('<a data-element-type="add-pipeline-script-param" class="float-right" href="#">Add parameter</a>'),
                $('<label class="col-form-label col-form-label-sm">').text('Parameters'),
                $('<ol id="list-pipeline-script-params-' + idSuffix + '">')
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-script-' + idSuffix).append(
                $('<label>').attr('for', 'input-pipeline-script-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Script').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<div>').addClass('input-group input-group-sm').append(
                        $('<input>').attr('id', 'input-pipeline-script-' + idSuffix)
                            .attr('type', 'text')
                            .attr('required', 'required')
                            .attr('data-required', 'required')
                            .attr('placeholder', 'params.param_1 / params.param_2')
                            .addClass('form-control form-control-sm')
                            .val(aggregatorData.script ? aggregatorData.script : defaultData.script)
                    )
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-lag-' + idSuffix).append(
                $('<label>').attr('for', 'input-pipeline-lag-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Lag').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-pipeline-lag-' + idSuffix)
                        .attr('type', 'number')
                        .attr('min', '1')
                        .attr('step', '1')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.lag ? aggregatorData.lag : defaultData.lag)
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-window-' + idSuffix).append(
                $('<label>').attr('for', 'input-pipeline-window-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Window').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-pipeline-window-' + idSuffix)
                        .attr('type', 'number')
                        .attr('min', '1')
                        .attr('step', '1')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.window ? aggregatorData.window : defaultData.window)
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-function-' + idSuffix).append(
                $('<label>').attr('for', 'sel-pipeline-function-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Function').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<select>').attr('id', 'sel-pipeline-function-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'pipeline-function-selector')
                        .addClass('form-control form-control-sm custom-select custom-select-sm')
                        .append(
                            $('<option>').attr('value', holtScript).text('Double exponential weighted average'),
                            $('<option>').attr('value', ewmaScript).text('Exponential weighted average'),
                            $('<option>').attr('value', 'MovingFunctions.linearWeightedAvg(values)').text('Linear weighted average'),
                            $('<option>').attr('value', 'MovingFunctions.max(values)').text('Max'),
                            $('<option>').attr('value', 'MovingFunctions.min(values)').text('Min'),
                            $('<option>').attr('value', 'MovingFunctions.stdDev(values, MovingFunctions.unweightedAvg(values))').text('Standard deviation'),
                            $('<option>').attr('value', 'MovingFunctions.sum(values)').text('Sum'),
                            $('<option>').attr('value', holtWinterScript).text('Triple exponential weighted average'),
                            $('<option>').attr('value', 'MovingFunctions.unweightedAvg(values)').text('Unweighted average')
                        )
                        .val(aggregatorData.script ? aggregatorData.script : defaultData.script)
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-alpha-' + idSuffix).append(
                $('<label>').attr('for', 'input-pipeline-alpha-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Alpha').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-pipeline-alpha-' + idSuffix)
                        .attr('type', 'number')
                        .attr('min', '0')
                        .attr('step', 'any')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.alpha ? aggregatorData.alpha : defaultData.alpha)
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-beta-' + idSuffix).append(
                $('<label>').attr('for', 'input-pipeline-beta-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Beta').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-pipeline-beta-' + idSuffix)
                        .attr('type', 'number')
                        .attr('min', '0')
                        .attr('step', 'any')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.beta ? aggregatorData.beta : defaultData.beta)
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-gamma-' + idSuffix).append(
                $('<label>').attr('for', 'input-pipeline-gamma-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Gamma').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-pipeline-gamma-' + idSuffix)
                        .attr('type', 'number')
                        .attr('min', '0')
                        .attr('step', 'any')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.gamma ? aggregatorData.gamma : defaultData.gamma)
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-period-' + idSuffix).append(
                $('<label>').attr('for', 'input-pipeline-period-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Periodicity').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<input>').attr('id', 'input-pipeline-period-' + idSuffix)
                        .attr('type', 'number')
                        .attr('min', '1')
                        .attr('step', '1')
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .attr('data-element-type', 'pipeline-periodicity')
                        .addClass('form-control form-control-sm')
                        .val(aggregatorData.period ? aggregatorData.period : defaultData.period)
                )
            ),
            $('<div style="display: none">').addClass('form-group row').attr('id', 'grp-pipeline-multiplicative-' + idSuffix).append(
                $('<label>').attr('for', 'sel-pipeline-multiplicative-' + idSuffix).addClass('col-sm-3 col-form-label col-form-label-sm').text('Multiplicative holt-winters').css('font-style', 'italic'),
                $('<div>').addClass('col-sm-9').append(
                    $('<select>').attr('id', 'sel-pipeline-multiplicative-' + idSuffix)
                        .attr('required', 'required')
                        .attr('data-required', 'required')
                        .addClass('form-control form-control-sm custom-select custom-select-sm')
                        .append(
                            $('<option>').attr('value', 'true').text('Yes'),
                            $('<option>').attr('value', 'false').text('No')
                        ).val(function () {
                        if (aggregatorData.multiplicative !== undefined) {
                            return aggregatorData.multiplicative ? 'true' : 'false';
                        }
                        return defaultData.multiplicative;
                    })
                )
            )
        );
        if (aggregatorData && aggregatorData.script_params) {
            const $paramList = $container.find("ol[id^=list-pipeline-script-params-]");
            $.each(aggregatorData.script_params, function (index, param) {
                $paramList.append(createPipelineScriptRow(param.name, param.aggregator_id, getPipelinePathSelectOptions($container, $parent)));
            });
        }
        $container.append(
            $('<a>').attr('href', '#').addClass('float-right')
                .attr('data-element-type', 'remove-pipeline-aggregator')
                .text('Remove pipeline'),
            $('<br>')
        );
        if (level > 0) {
            const $bucketShowOnGraph = $parent.find("> div[data-aggregator-ix='-1'] > div > div > select[data-element-type='show-on-graph-selector']");
            if ($bucketShowOnGraph.val() === 'false' || $bucketShowOnGraph.attr('disabled') !== undefined) {
                $container.find("select[data-element-type='show-on-graph-selector']").attr('disabled', 'disabled');
            }
        }
        $parent.append($container);

        const $pipelinePathSelector = $container.find('select[data-element-type=pipeline-path]');
        const pipelinePathValue = aggregatorData.path ? aggregatorData.path : defaultData.path;
        if (pipelinePathValue && pipelinePathValue !== $pipelinePathSelector.val()) {
            // The value that should be set could not be set on the select element because the correct option isn't available yet. We add a dummy option here so the updatePipelinePathSelectOptions method can set the correct value later on.
            $pipelinePathSelector.append(
                $('<option>').attr('value', pipelinePathValue).text('Unknown')
            );
            $pipelinePathSelector.val(pipelinePathValue);
        }
        updatePipelinePathSelectOptions($pipelinePathSelector);

    };

    const createAggregatorData = function createAggregatorBlockData(dataContext, $aggregatorBlock) {
        const level = $aggregatorBlock.attr('data-aggregator-level');
        const $aggregators = $aggregatorBlock.children('[data-aggregator-ix]').sort(function (item1, item2) {
            const ix1 = Number($(item1).attr('data-aggregator-ix'));
            const ix2 = Number($(item2).attr('data-aggregator-ix'));
            if (ix1 > ix2) {
                return 1;
            } else if (ix1 < ix2) {
                return -1;
            }
            return 0;
        });
        const data = [];
        $aggregators.each(function (index, item) {
            const $aggBlock = $(item);
            const ix = Number($aggBlock.attr('data-aggregator-ix'));
            if (-1 === ix) {
                return true;
            }
            const idSuffix = dataContext + '-' + level + '-' + ix;
            const aggData = {};
            if ($aggBlock.hasClass('metrics-aggregator-block')) {
                aggData.id = level + '-' + ix;
                aggData.name = $aggBlock.find('#input-metrics-name-' + idSuffix).val();
                aggData.type = 'metrics';
                aggData.show_on_graph = $aggBlock.find('#sel-metrics-show-on-graph-' + idSuffix).val() === 'true';
                aggData.metrics_type = $aggBlock.find('#sel-metrics-aggregator-' + idSuffix).val();
                if ('count' !== aggData.metrics_type && 'scripted' !== aggData.metrics_type) {
                    aggData.field = $aggBlock.find('#input-metrics-field-' + idSuffix).val();
                    if ('percentile' === aggData.metrics_type) {
                        aggData.percentile = Number($aggBlock.find('#input-metrics-percentile-' + idSuffix).val());
                    } else if ('percentile_rank' === aggData.metrics_type) {
                        aggData.rank = Number($aggBlock.find('#input-metrics-percentile-rank-' + idSuffix).val());
                    }
                } else if ('scripted' === aggData.metrics_type) {
                    const initScript = $aggBlock.find('#input-metrics-scripted-init-' + idSuffix).val();
                    const combineScript = $aggBlock.find('#input-metrics-scripted-combine-' + idSuffix).val();
                    const reduceScript = $aggBlock.find('#input-metrics-scripted-reduce-' + idSuffix).val();
                    aggData.init_script = initScript ? initScript : null;
                    aggData.map_script = $aggBlock.find('#input-metrics-scripted-map-' + idSuffix).val();
                    aggData.combine_script = combineScript ? combineScript : null;
                    aggData.reduce_script = reduceScript ? reduceScript : null;
                }
            } else if ($aggBlock.hasClass('pipeline-aggregator-block')) {
                aggData.id = level + '-' + ix;
                aggData.name = $aggBlock.find('#input-pipeline-name-' + idSuffix).val();
                aggData.type = 'pipeline';
                aggData.show_on_graph = $aggBlock.find('#sel-pipeline-show-on-graph-' + idSuffix).val() === 'true';
                aggData.pipeline_type = $aggBlock.find('#sel-pipeline-aggregator-' + idSuffix).val();
                if ('scripted' === aggData.pipeline_type) {
                    aggData.script_params = [];
                    $aggBlock.find('#list-pipeline-script-params-' + idSuffix + ' > li').each(function (liIx, liItem) {
                        aggData.script_params.push({
                            name: $(liItem).find('input').val(),
                            aggregator_id: $(liItem).find('select').val()
                        });
                    });
                    aggData.script = $aggBlock.find('#input-pipeline-script-' + idSuffix).val();
                } else {
                    aggData.path = $aggBlock.find('#sel-pipeline-path-' + idSuffix).val();
                }
                if ('serial_diff' === aggData.pipeline_type) {
                    aggData.lag = Number($aggBlock.find('#input-pipeline-lag-' + idSuffix).val());
                } else if ('moving_function' === aggData.pipeline_type) {
                    aggData.window = Number($aggBlock.find('#input-pipeline-window-' + idSuffix).val());
                    aggData.script = $aggBlock.find('#sel-pipeline-function-' + idSuffix).val();
                    if ($aggBlock.find('#grp-pipeline-alpha-' + idSuffix).css('display') !== 'none') {
                        aggData.alpha = Number($('#input-pipeline-alpha-' + idSuffix).val());
                    }
                    if ($aggBlock.find('#grp-pipeline-beta-' + idSuffix).css('display') !== 'none') {
                        aggData.beta = Number($('#input-pipeline-beta-' + idSuffix).val());
                    }
                    if ($aggBlock.find('#grp-pipeline-gamma-' + idSuffix).css('display') !== 'none') {
                        aggData.gamma = Number($('#input-pipeline-gamma-' + idSuffix).val());
                        aggData.period = Number($('#input-pipeline-period-' + idSuffix).val());
                        aggData.multiplicative = $('#sel-pipeline-multiplicative-' + idSuffix).val() === 'true';
                    }
                }
            } else if ($aggBlock.hasClass('aggregator-container-block')) {
                aggData.id = level + '-' + ix;
                aggData.name = $aggBlock.find('#input-bucket-name-' + idSuffix).val();
                aggData.type = 'bucket';
                aggData.show_on_graph = $aggBlock.find('#sel-bucket-show-on-graph-' + idSuffix).val() === 'true';
                aggData.bucket_type = $aggBlock.find('#sel-bucket-aggregator-' + idSuffix).val();
                aggData.field = $aggBlock.find('#input-bucket-field-' + idSuffix).val();
                aggData.field_type = findFieldType($('#sel-data-source').val(), aggData.field);
                const minNrOfDocuments = $aggBlock.find('#input-bucket-min-doc-count-' + idSuffix).val() ? Number($aggBlock.find('#input-bucket-min-doc-count-' + idSuffix).val()) : null;
                if ('date_histogram' === aggData.bucket_type) {
                    aggData.interval = $aggBlock.find('#sel-bucket-date-interval-' + idSuffix).val();
                    aggData.min_doc_count = minNrOfDocuments;
                } else if ('filter' === aggData.bucket_type) {
                    aggData.value = $aggBlock.find('#input-bucket-filter-value-' + idSuffix).val();
                } else if ('histogram' === aggData.bucket_type) {
                    aggData.interval = Number($aggBlock.find('#input-bucket-histogram-interval-' + idSuffix).val());
                    aggData.min_doc_count = minNrOfDocuments;
                } else if ('significant_term' === aggData.bucket_type) {
                    aggData.top = Number($aggBlock.find('#input-bucket-term-top-' + idSuffix).val());
                    aggData.min_doc_count = minNrOfDocuments;
                } else if ('term' === aggData.bucket_type) {
                    aggData.top = Number($aggBlock.find('#input-bucket-term-top-' + idSuffix).val());
                    aggData.order_by = $aggBlock.find('#sel-bucket-term-order-by-' + idSuffix).val();
                    aggData.order = $aggBlock.find('#sel-bucket-term-order-' + idSuffix).val();
                    aggData.min_doc_count = minNrOfDocuments;
                }
                aggData.aggregators = createAggregatorBlockData(dataContext, $aggBlock);
            }
            data.push(aggData);
        });
        return data;
    };

    const validateNumberOfMetricsAndPipelines = function validateNumberOfMetricsAndPipelines(formId) {
        const min = 1;
        let max = MAX_SAFE_INTEGER;
        const $form = $('#' + formId);
        const $rootContainer = $form.find('div.aggregator-container-block[data-aggregator-level="0"]');
        if ('true' === $rootContainer.attr('data-require-single-value')) {
            max = 1;
        }
        const numberCount = $form.find('select[id^=sel-metrics-show-on-graph-]:enabled, select[id^=sel-pipeline-show-on-graph-]:enabled').filter(function (index, element) {
            return 'true' === $(element).val();
        }).length;
        return max >= numberCount && numberCount >= min;
    };

    return {
        initialize: initialize,
        addBucketAggregator: addBucketAggregator,
        addMetricsAggregator: addMetricsAggregator,
        addPipelineAggregator: addPipelineAggregator,
        createAggregatorData: createAggregatorData,
        validateNumberOfMetricsAndPipelines: validateNumberOfMetricsAndPipelines
    };
})();