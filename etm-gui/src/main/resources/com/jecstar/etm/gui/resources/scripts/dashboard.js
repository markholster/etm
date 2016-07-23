function loadDashboard(name) {
	var currentDashboard = {
			name: 'My Dashboard',
			rows: [
			  { height: 16,
				cols: [
				  { parts: 6,
					title: 'Test colom 1',
					bordered: true,
					type: 'bar'
				  },
				  { parts: 6,
					title: 'Test colom 2',
					bordered: true,
					type: 'pie'
				  }				  
				]
			  }
			]	
	}
	if (name) {
		// TODO load dashboard into currentDashboard
	}
	buildPage(currentDashboard);
	
	$('#lnk-edit-dashboard').click(function (event) {
		event.preventDefault();
		$('#input-dashboard-name').val(currentDashboard.name);
		$('#modal-dashboard-settings').modal();
	});
	
	$('#btn-apply-dashboard-settings').click(function (event) {
		currentDashboard.name = $('#input-dashboard-name').val();
		$('#modal-dashboard-settings').modal('hide');
		buildPage(currentDashboard);
	});
	
	function buildPage(board) {
		$('#dashboard-name').text(board.name);
		var graphContainer = $('#graph-container').empty();
		$.each(board.rows, function(rowIx, row) {
			var rowContainer = $('<div>').addClass('row').attr('style', 'height: ' + row.height + 'rem;');
			$.each(row.cols, function (colIx, col) {
				var colContainer = $('<div>').addClass('col-md-' + col.parts).attr('style', 'height: 100%;');
				var svgContainer = colContainer;
				if (col.bordered) {
					var card = $('<div>').addClass('card card-block').attr('style', 'height: 100%;');
					svgContainer = card;
					colContainer.append(card);
				}
				svgContainer.append(
				  $('<h5>').addClass('card-title').text(col.title).append(
				    $('<i>').addClass('fa fa-pencil-square-o pull-right')
				  )
				);
				rowContainer.append(colContainer);
			});
			graphContainer.append(rowContainer);
		});
	}
}