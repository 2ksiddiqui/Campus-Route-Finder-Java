import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

public class CampusRouteFinder extends JFrame {
	static class Edge {
		String destination;
		double distance;

		Edge(String destination, double distance) {
			this.destination = destination;
			this.distance = distance;
		}
	}

	static class Location implements Comparable<Location> {
		String name;
		double distance;
		Location(String name, double distance) {
			this.name = name;
			this.distance = distance;
		}

		@Override
		public int compareTo(Location other) {
			return Double.compare(this.distance, other.distance);
		}
	}

	private final Color DARK_BG =  new Color(15, 23, 42);
	private final Color PANEL_BG = new Color(30, 41, 59);
	private final Color ACCENT_BLUE = new Color(59, 130, 246);
	private final Color TEXT_WHITE = new Color(248, 250, 252);

	private final Map<String, List<Edge>> campusMap = new HashMap<>();

	private final String[] locations = {
		"Library",
		"Student Center",
		"Engineering Hall",
		"Science Building",
		"Recreation Center"
	};

	private JComboBox<String> startBox;
	private JComboBox<String> destinationBox;
	private JTextField searchField;
	private JList<String> suggestionList;
	private JScrollPane suggestionScroll;
	private DefaultListModel<String> suggestionModel;

	private JLabel routeLabel;
	private JLabel distanceLabel;
	private JLabel timeLabel;
	private JLabel explanationLabel;
	private JLabel breakdownLabel;
	private JLabel historyLabel;

	private MapPanel mapPanel;

	private List<String> currentRoute = new ArrayList<>();
	private List<String> routeHistory = new ArrayList<>();
	private List<Double> routeDistances = new ArrayList<>();

	public CampusRouteFinder() {
		createCampusMap();

		setTitle("Campus Route Finder | Dijkstra's Shortest Path");
		setSize(1200, 800);
		setMinimumSize(new Dimension(1000, 700));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		getContentPane().setBackground(DARK_BG);

		createHeader();
		createControlPanel();
		createMapPanel();
		createResultsPanel();

		setVisible(true);
	}

	private void createCampusMap() {
		for (String location : locations) {
			campusMap.put(location, new ArrayList<>());
	}

	addPath("Library", "Student Center", 0.3);
	addPath("Library", "Science Building", 0.5);

	addPath("Student Center", "Engineering Hall", 0.4);
	addPath("Student Center", "Recreation Center", 0.6);

	addPath("Science Building", "Engineering Hall", 0.3);
	addPath("Science Building", "Recreation Center", 0.5);

	addPath("Engineering Hall", "Recreation Center", 0.4);
}

private void addPath(String first, String second, double distance) {
	campusMap.get(first).add(new Edge(second, distance));
	campusMap.get(second).add(new Edge(first, distance));
}

private void createHeader() {
	JPanel header = new JPanel();
	header.setBackground(DARK_BG);
	header.setBorder(new EmptyBorder(20, 10, 20, 10));
	JLabel title = new JLabel("CAMPUS ROUTE FINDER");
	title.setForeground(Color.WHITE);
	title.setFont(new Font("Arial", Font.BOLD, 28));

	header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
	title.setAlignmentX(Component.CENTER_ALIGNMENT);
	JLabel subtitle = new JLabel(
		"Find the fastest walking route between campus buildings"
	);
	subtitle.setForeground(new Color(148, 163, 184));
	subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
	subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
	header.add(title);
	header.add(Box.createRigidArea(new Dimension(0, 8)));
	header.add(subtitle);

	add(header, BorderLayout.NORTH);
}


private void createControlPanel() {
	JPanel controlPanel = new JPanel();
	controlPanel.setBackground(PANEL_BG);

	controlPanel.setLayout(
		new BoxLayout(controlPanel, BoxLayout.Y_AXIS)
	);


	controlPanel.setBorder(
		new EmptyBorder(20, 20, 20, 20)
		);

		searchField = new JTextField();
		suggestionModel = new DefaultListModel<>();
		suggestionList = new JList<>(suggestionModel);
		suggestionList.setBackground(new Color(51, 65, 85));
		suggestionList.setForeground(TEXT_WHITE);
		suggestionList.setFont(new Font("Arial", Font.PLAIN, 13));
		suggestionList.setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION
		);
		suggestionList.setVisibleRowCount(3);
		suggestionScroll =  new JScrollPane(suggestionList);
		suggestionScroll.setMaximumSize(new Dimension(200, 85));
		suggestionScroll.setPreferredSize(new Dimension(200, 85));
		suggestionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		suggestionScroll.setVisible(false);
		searchField.setMaximumSize(new Dimension(200,35));
		searchField.setFont(new Font("Arial", Font.PLAIN, 14));
		searchField.setToolTipText("Search for a campus building");
		searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
		searchField.setBackground(new Color(51, 65, 85));
		searchField.setForeground(Color.WHITE);
		searchField.setCaretColor(Color.WHITE);
		searchField.setBorder(
		    BorderFactory.createEmptyBorder(8, 10, 8, 10)
		);
		JButton searchButton = new JButton("Search Building");
		searchButton.setBackground(ACCENT_BLUE);
		searchButton.setForeground(TEXT_WHITE);
		searchButton.setFont(new Font("Arial", Font.BOLD, 14));
		searchButton.setFocusPainted(false);
		searchButton.setMaximumSize(new Dimension(200, 40));
		searchButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		searchButton.addActionListener(e -> searchBuilding());
		searchField.getDocument().addDocumentListener(
			new javax.swing.event.DocumentListener() {
				public void insertUpdate(javax.swing.event.DocumentEvent e) {
					updateSuggestions();
				}
				public void removeUpdate(javax.swing.event.DocumentEvent e) {
					updateSuggestions();
				}
				public void changedUpdate(javax.swing.event.DocumentEvent e) {
					updateSuggestions();
				}
			}
		);

		suggestionList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				String selected = suggestionList.getSelectedValue();
				if (selected != null) {
					destinationBox.setSelectedItem(selected);
				}
			}
		});
		JLabel sectionTitle = new JLabel("PLAN YOUR ROUTE");
		sectionTitle.setFont(new Font("Arial", Font.BOLD, 18));
		sectionTitle.setForeground(ACCENT_BLUE);
		sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel startLabel = new JLabel("Starting Location");
		startLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		startLabel.setFont(new Font("Arial", Font.BOLD, 14));
		startLabel.setForeground(TEXT_WHITE);

		startBox = new JComboBox <>(locations);
		startBox.setMaximumSize(new Dimension(200, 35));
		startBox.setBackground(new Color(51, 65, 85));
		startBox.setForeground(TEXT_WHITE);
		startBox.setFont(new Font("Arial", Font.PLAIN, 14));
		startBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		startBox.setBorder(
		    BorderFactory.createEmptyBorder(5, 8, 5, 8)
		);
		JLabel destinationLabel = new JLabel("Destination");
		destinationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		destinationLabel.setFont(new Font("Arial", Font.BOLD, 14));
		destinationLabel.setForeground(TEXT_WHITE);

		destinationBox = new JComboBox<>(locations);
		destinationBox.setMaximumSize(new Dimension(200, 35));
		destinationBox.setBackground(new Color(51, 65, 85));
		destinationBox.setForeground(TEXT_WHITE);
		destinationBox.setFont(new Font("Arial", Font.PLAIN, 14));
		destinationBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		destinationBox.setBorder(
		    BorderFactory.createEmptyBorder(5, 8, 5, 8)
		);

		JButton swapButton = new JButton("Swap Locations");
		swapButton.setBackground(new Color(51, 65, 85));
		swapButton.setForeground(TEXT_WHITE);
		swapButton.setFont(new Font("Arial", Font.BOLD, 14));
		swapButton.setFocusPainted(false);
		swapButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		swapButton.setMaximumSize(new Dimension(200, 40));
		swapButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		swapButton.addActionListener(e -> swapLocations());

		JButton findButton = new JButton("Find Shortest Route") {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON
				);
				g2.setColor(getBackground());
				g2.fillRoundRect(
					0, 0, getWidth(), getHeight(), 18, 18
				);

				g2.dispose();
				super.paintComponent(g);
			}
		};
		findButton.setBackground(ACCENT_BLUE);
		findButton.setForeground(Color.WHITE);
		findButton.setFocusPainted(false);
		findButton.setBorderPainted(false);
		findButton.setContentAreaFilled(false);
		findButton.setOpaque(false);
		findButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		findButton.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				findButton.setBackground(new Color(37, 99, 235));
		}

		@Override
		public void mouseExited(java.awt.event.MouseEvent e) {
			findButton.setBackground(ACCENT_BLUE);
		}
	});

		findButton.setFont(
			new Font("Arial", Font.BOLD, 14)
		);

		findButton.setMaximumSize(
			new Dimension(200, 40)
		);
		findButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton resetButton = new JButton("Reset") {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON
				);

				g2.setColor(getBackground());
				g2.fillRoundRect(
					0, 0, getWidth(), getHeight(), 18, 18
				);

				g2.dispose();
				super.paintComponent(g);
			}
			};
			resetButton.setBackground(new Color(51, 65, 85));
			resetButton.setForeground(TEXT_WHITE);
			resetButton.setFocusPainted(false);
			resetButton.setBorderPainted(false);
			resetButton.setContentAreaFilled(false);
			resetButton.setOpaque(false);
			resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
			resetButton.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseEntered(java.awt.event.MouseEvent e) {
					resetButton.setBackground(new Color(71, 85, 105));
				}
				@Override
				public void mouseExited(java.awt.event.MouseEvent e) {
					resetButton.setBackground(new Color(51, 65, 85));
				}
			});

		resetButton.setMaximumSize(
			new Dimension(200, 40)
		);
		resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		findButton.addActionListener(e -> findRoute());
		resetButton.addActionListener(e -> resetRoute());
		JButton clearHistoryButton = new JButton("Clear History");
		clearHistoryButton.setBackground(new Color(51, 65, 85));
		clearHistoryButton.setForeground(TEXT_WHITE);
		clearHistoryButton.setFont(new Font("Arial", Font.BOLD, 14));
		clearHistoryButton.setFocusPainted(false);
		clearHistoryButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		clearHistoryButton.setMaximumSize(
			new Dimension (200, 40)
		);
		clearHistoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		clearHistoryButton.addActionListener(e -> clearHistory());
		JButton compareButton = new JButton("Compare Routes");
		compareButton.setBackground(ACCENT_BLUE);
		compareButton.setForeground(TEXT_WHITE);
		compareButton.setFont(new Font("Arial", Font.BOLD, 14));
		compareButton.setFocusPainted(false);
		compareButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		compareButton.setMaximumSize(
			new Dimension(200, 40)
		);
		compareButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		compareButton.addActionListener(e -> compareRoutes());
		JButton exportButton = new JButton("Export Route");
		exportButton.setBackground(new Color(51, 65, 85));
		exportButton.setForeground(TEXT_WHITE);
		exportButton.setFont(new Font("Arial", Font.BOLD, 14));
		exportButton.setFocusPainted(false);
		exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		exportButton.setMaximumSize(new Dimension(200, 40));
		exportButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		exportButton.addActionListener(e -> exportRoute());
		styleButton(searchButton, ACCENT_BLUE);
		styleButton(swapButton, new Color(51, 65, 85));
		styleButton(clearHistoryButton, new Color(51, 65, 85));
		styleButton(compareButton, ACCENT_BLUE);
		styleButton(exportButton, new Color(51, 65, 85));
		controlPanel.add(searchField);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		controlPanel.add(suggestionScroll);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		controlPanel.add(searchButton);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		controlPanel.add(sectionTitle);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 25)));
		controlPanel.add(startLabel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		controlPanel.add(startBox);

		controlPanel.add(Box.createRigidArea(new Dimension(0, 25)));

		controlPanel.add(destinationLabel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		controlPanel.add(destinationBox);

		controlPanel.add(Box.createRigidArea(new Dimension(0, 15)));
		controlPanel.add(swapButton);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 15)));
		controlPanel.add(findButton);

		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		controlPanel.add(resetButton);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		controlPanel.add(clearHistoryButton);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		controlPanel.add(compareButton);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		controlPanel.add(exportButton);



		JScrollPane sidebarScroll = new JScrollPane(controlPanel);
		sidebarScroll.setPreferredSize(new Dimension(260, 0));
		sidebarScroll.setBorder(null);
		sidebarScroll.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);
		sidebarScroll.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
		);
		sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);
		add(sidebarScroll, BorderLayout.WEST);
	}


	private void createMapPanel() {
		JPanel mapContainer = new JPanel(new BorderLayout());
		mapContainer.setBackground(DARK_BG);
		JLabel mapInstruction = new JLabel(
			"CAMPUS MAP | Hover to highlight • Click to select a destination"
		);
		mapInstruction.setForeground(new Color(148, 163, 184));
		mapInstruction.setFont(new Font("Arial", Font.PLAIN, 13));
		mapInstruction.setBorder(
			new EmptyBorder(12, 15, 12, 15)
		);
		mapPanel = new MapPanel();
		mapPanel.setBackground(DARK_BG);
		mapPanel.setBorder(
			BorderFactory.createLineBorder(
				new Color(51, 65, 85), 1
		    )
		);
		mapContainer.add(mapInstruction, BorderLayout.NORTH);
		mapContainer.add(mapPanel, BorderLayout.CENTER);
		add(mapContainer, BorderLayout.CENTER);
	}

	private void createResultsPanel() {
		JPanel results = new JPanel();
		results.setBackground(PANEL_BG);
		results.setLayout(
			new BoxLayout(results, BoxLayout.Y_AXIS)
		);

		results.setBorder(
			new EmptyBorder(18, 25, 18, 25)
		);

		JLabel resultsTitle = new JLabel("YOUR ROUTE SUMMARY");
		resultsTitle.setFont(new Font("Arial", Font.BOLD, 18));
		resultsTitle.setForeground(ACCENT_BLUE);
		resultsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		routeLabel = new JLabel(
			"Route: Select two locations."
		);
		distanceLabel = new JLabel(
			"Distance: --"
		);

		timeLabel = new JLabel(
				"Walking Time: --"
		);

		explanationLabel = new JLabel(
			"Algorithm: Waiting for route selection."
		);
		explanationLabel.setFont(new Font("Arial", Font.PLAIN, 13));
		explanationLabel.setForeground(new Color(203, 213, 225));
		explanationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		breakdownLabel = new JLabel(
			"<html><b>ROUTE BREAKDOWN</b><br>--</html>"
		);
		breakdownLabel.setFont(new Font("Arial", Font.PLAIN, 13));
		breakdownLabel.setForeground(TEXT_WHITE);
		breakdownLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		historyLabel = new JLabel(
			"<html><b>RECENT ROUTES</b><br>No routes searched yet.</html>"
		);
		historyLabel.setFont(new Font("Arial", Font.PLAIN, 13));
		historyLabel.setForeground(TEXT_WHITE);
		historyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		breakdownLabel.setMaximumSize(
			new Dimension(650, Integer.MAX_VALUE)
		);

		routeLabel.setFont(
			new Font("Arial", Font.BOLD, 16)
		);
		distanceLabel.setFont(
			new Font("Arial", Font.PLAIN, 14)
		);
		timeLabel.setFont(
			new Font("Arial", Font.PLAIN, 14)
		);

		routeLabel.setForeground(TEXT_WHITE);
		distanceLabel.setForeground(TEXT_WHITE);
		timeLabel.setForeground(TEXT_WHITE);
		routeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		distanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		results.add(resultsTitle);
		results.add(Box.createRigidArea(new Dimension(0, 12)));
		results.add(routeLabel);
		results.add(Box.createRigidArea(new Dimension(0, 12)));
		JPanel statsPanel = new JPanel();
		statsPanel.setLayout(new GridLayout(1, 2, 15, 0));
		statsPanel.setBackground(PANEL_BG);
		statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel distancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON
				);
				g2.setColor(getBackground());
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
				g2.dispose();
			}
		};
		distancePanel.setOpaque(false);
		distancePanel.setBackground(new Color(51, 65, 85));
		distancePanel.setBorder(
		    BorderFactory.createEmptyBorder(6, 12, 6, 12)
		);
		JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON
					);
					g2.setColor(getBackground());
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
					g2.dispose();
				}
			};
			timePanel.setOpaque(false);

		timePanel.setBackground(new Color(51, 65, 85));
		timePanel.setBorder(
		    BorderFactory.createEmptyBorder(6, 12, 6, 12)
		);
		distancePanel.add(distanceLabel);
		timePanel.add(timeLabel);
		statsPanel.add(distancePanel);
		statsPanel.add(timePanel);
		results.add(statsPanel);
		results.add(Box.createRigidArea(new Dimension(0, 12)));
		results.add(explanationLabel);
		results.add(Box.createRigidArea(new Dimension(0, 8)));
		results.add(breakdownLabel);
		results.add(Box.createRigidArea(new Dimension(0, 12)));
		results.add(historyLabel);
		JScrollPane resultsScroll = new JScrollPane(results);
		resultsScroll.setBorder(null);
		resultsScroll.setBackground(PANEL_BG);
		resultsScroll.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);
		resultsScroll.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
		);
		resultsScroll.setPreferredSize(new Dimension(0, 180));
		add(resultsScroll, BorderLayout.SOUTH);
	}

	private void findRoute() {
		String start =
			(String) startBox.getSelectedItem();
		String destination =
			(String) destinationBox.getSelectedItem();
		if (start.equals(destination)) {

			JOptionPane.showMessageDialog(
				this,
				"Please choose two different locations.",
				"Same Location",
				JOptionPane.WARNING_MESSAGE
			);
			return;
		}

		Map<String, Double> distances = new HashMap<>();
		Map<String, String> previous = new HashMap<>();

		PriorityQueue<Location> queue =
			new PriorityQueue<>();
		for (String location: campusMap.keySet()) {
			distances.put(location, Double.MAX_VALUE);
		}

		distances.put(start, 0.0);

		queue.add(
			new Location(start, 0.0)
		);

		while (!queue.isEmpty()) {
			Location current = queue.poll();

			if (current.distance >
					distances.get(current.name)) {
				continue;
			}

			if(current.name.equals(destination)) {
				break;
			}

			for(Edge edge :
					campusMap.get(current.name)) {
				double newDistance =
						current.distance + edge.distance;
				if (newDistance <
						distances.get(edge.destination)) {
					distances.put(
							edge.destination,
							newDistance
					);

					previous.put(
							edge.destination,
							current.name
					);

					queue.add(
							new Location(
									edge.destination,
									newDistance
							)
					);
				}
			}
		}

		currentRoute = new ArrayList<>();
		String current = destination;

		while (current != null) {
			currentRoute.add(current);
			current = previous.get(current);
		}

		Collections.reverse(currentRoute);
		String savedRoute = String.join(" → ", currentRoute);
		routeHistory.add(savedRoute);
		StringBuilder breakdown = new StringBuilder(
			"<html><b>ROUTE BREAKDOWN</b><br>"
		);
		for (int i = 0; i < currentRoute.size() - 1; i++) {
			String from = currentRoute.get(i);
			String to = currentRoute.get(i + 1);
		for (Edge edge : campusMap.get(from)) {
			if (edge.destination.equals(to)) {
				breakdown.append(
					String.format(
						"%s to %s: %.2f mi",
						from, to, edge.distance
					)
				);
				if (i < currentRoute.size() - 2) {
					breakdown.append("<br>");
				}
				break;
			}
		}
	}

	breakdown.append("</html>");
	breakdownLabel.setText(
		breakdown.toString()
	);


		double totalDistance =
					  distances.get(destination);
					  routeDistances.add(totalDistance);
		double walkingMinutes =
				(totalDistance / 3.0) * 60;
		routeLabel.setText(
				"Route: " +
						String.join("   →    ", currentRoute)
		);

		distanceLabel.setText(
				String.format(
						"Distance: %.2f miles",
						totalDistance
				)
		);

		timeLabel.setText(
				String.format(
						"Walking Time: %.0f minutes",
						walkingMinutes
				)
		);

		explanationLabel.setText(
			"Dijkstra's algorithm selected the shortest path: "
			+ String.format("%.2f miles.", totalDistance)
		);
		StringBuilder history = new StringBuilder(
			"<html><b>RECENT ROUTES</b><br>"
		);
		int startIndex = Math.max(0, routeHistory.size() - 3);
		for (int i = routeHistory.size() - 1; i >= startIndex; i --) {
			history.append(routeHistory.get(i));
			if(i > startIndex) {
				history.append("<br>");
			}
		}
		history.append("</html>");
		historyLabel.setText(history.toString());
		mapPanel.repaint();
	}

	private void swapLocations() {
		String start = (String) startBox.getSelectedItem();
		String destination = (String) destinationBox.getSelectedItem();
		startBox.setSelectedItem(destination);
		destinationBox.setSelectedItem(start);
	}

	private void resetRoute() {
		startBox.setSelectedIndex(0);
		destinationBox.setSelectedIndex(0);
		searchField.setText("");
		suggestionModel.clear();

		currentRoute.clear();
		routeLabel.setText(
				"Route: Select two locations."
		);

		distanceLabel.setText(
				"Distance: --"
		);

		timeLabel.setText(
				"Walking Time: --"
		);

		explanationLabel.setText(
			"Algorithm: Waiting for route selection."
		);

		breakdownLabel.setText(
			"<html><b>ROUTE BREAKDOWN</b><br>--</html>"
		);
		mapPanel.repaint();
	}

	private void clearHistory() {
		routeHistory.clear();
		routeDistances.clear();
		historyLabel.setText(
			"<html><b>RECENT ROUTES</b> No routes searched yet.</html>"
		);
	}

	private void compareRoutes() {
		if (routeHistory.size() < 2) {
			JOptionPane.showMessageDialog(
				this,
				"Please search at least two routes first.",
				"Not Enough Routes",
				JOptionPane.INFORMATION_MESSAGE
			);
			return;
		}
		int last = routeHistory.size() - 1;
		int previous = last - 1;
		double distance1 = routeDistances.get(previous);
		double distance2 = routeDistances.get(last);
		double time1 = (distance1 / 3.0) * 60;
		double time2 = (distance2 / 3.0) * 60;
		String message = String.format(
			"ROUTE 1: %s\nDistance: %.2f miles\nWalking Time: %.0f minutes\n\n"
			+ "ROUTE 2: %s\nDistance: %.2f miles\nWalking Time: %.0f minutes\n\n",
			routeHistory.get(previous), distance1, time1,
			routeHistory.get(last), distance2, time2
		);
		if (distance1 < distance2) {
			message += "Route 1 is shorter!";
		} else if (distance2 < distance1) {
			message += "Route 2 is shorter!";
		} else {
			message += "Both routes have the same distance!";
		}
		JOptionPane.showMessageDialog(
			this,
			message,
			"Route Comparison",
			JOptionPane.INFORMATION_MESSAGE
		);
	}

	private void exportRoute() {
		if (currentRoute.isEmpty()) {
			JOptionPane.showMessageDialog(
				this,
				"Please find a route first."
			);
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File("campus_route.txt"));
		int result = chooser.showSaveDialog(this);
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = chooser.getSelectedFile();
		if(file.exists()) {
			int choice = JOptionPane.showConfirmDialog(
				this,
				"This file already exists. Replace it?",
				"Confirm Overwrite",
				JOptionPane.YES_NO_OPTION
			);
			if (choice != JOptionPane.YES_OPTION) {
				return;
			}
		}

		try (PrintWriter writer = new PrintWriter(file)) {
			writer.println("CAMPUS ROUTE FINDER");
			writer.println("----------------------------");
			writer.println(
				"Route: " + String.join(" -> ", currentRoute)
			);
			writer.println(distanceLabel.getText());
			writer.println(timeLabel.getText());
			writer.println();
			writer.println("Algorithm: Dijkstra's Shortest Path");
			JOptionPane.showMessageDialog(
				this,
				"Route exported successfully!"
			);
		}  catch (IOException e) {
			JOptionPane.showMessageDialog(
				this,
				"Error saving route: " + e.getMessage()
			);
		}
	}


	private void styleButton(JButton button, Color color) {

	    button.setBackground(color);
	    button.setForeground(Color.WHITE);

	    button.setFont(
	        new Font("Arial", Font.BOLD, 13)
	    );

	    button.setFocusPainted(false);
	    button.setCursor(
	        new Cursor(Cursor.HAND_CURSOR)
	    );

	    button.setBorder(
	        BorderFactory.createEmptyBorder(10, 12, 10, 12)
	    );
	}


	private void searchBuilding() {
		String search = searchField.getText().trim();
		if(search.isEmpty()) {
			JOptionPane.showMessageDialog(
				this,
				"Please enter a building name."
			);
			return;
		}
		for (String location : locations) {
			if (location.toLowerCase().contains(search.toLowerCase())) {
				destinationBox.setSelectedItem(location);
				JOptionPane.showMessageDialog(
					this,
					"Building found: " + location
				);
				return;
			}
		}
		JOptionPane.showMessageDialog(
			this,
			"Building not found."
		);
	}

	private void updateSuggestions() {
		suggestionModel.clear();
		String search = searchField.getText()
				.trim().toLowerCase();
		if (!search.isEmpty()) {
			for (String location : locations) {
				if (location.toLowerCase().contains(search)) {
					suggestionModel.addElement(location);
				}
			}
		}
		suggestionScroll.setVisible(
			suggestionModel.getSize() > 0
		);
		suggestionScroll.revalidate();
	}




	class MapPanel extends JPanel {
		private final Map<String, Point> positions =
				new HashMap<>();
		private int getMapOffsetX() {
			return (getWidth() - 690) / 2;
		}
		private int getMapOffsetY() {
			return (getHeight() - 480) / 2;
		}
		private String hoveredBuilding = null;
		MapPanel() {
			positions.put(
					"Library",
					new Point(120, 130)
			);

			positions.put(
					"Student Center",
					new Point(350, 100)
			);

			positions.put(
					"Engineering Hall",
					new Point(570, 210)
			);

			positions.put(
					"Science Building",
					new Point(250, 330)
			);

			positions.put(
					"Recreation Center",
					new Point(520, 390)
			);


		addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				selectBuilding(new Point(
					e.getX() - getMapOffsetX(),
					e.getY() - getMapOffsetY()
				));
			}
		});
		addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			@Override
			public void mouseMoved(java.awt.event.MouseEvent e) {
				hoveredBuilding = null;
				int mouseX = e.getX() - getMapOffsetX();
				int mouseY = e.getY() - getMapOffsetY();
				for (String building : positions.keySet()) {
					Point point = positions.get(building);
					if (Math.abs(mouseX - point.x) <= 72
							&& Math.abs(mouseY - point.y) <= 27) {
						hoveredBuilding = building;
						break;
					}
				}
				setCursor(new Cursor(
					hoveredBuilding != null
						? Cursor.HAND_CURSOR
						: Cursor.DEFAULT_CURSOR
				));
				repaint();
			}
		});
		}


		private void selectBuilding(Point click) {
			for (String building : positions.keySet()) {
				Point point = positions.get(building);
				int width = 145;
				int height = 55;
				int left = point.x - width / 2;
				int top = point.y - height / 2;
				if (click.x >= left && click.x <= left + width
						&& click.y >= top && click.y <= top + height) {
					destinationBox.setSelectedItem(building);
					JOptionPane.showMessageDialog(
						CampusRouteFinder.this,
						"Destination selected: " + building
					);
					return;
				}
			}
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON
			);
			g2.translate(getMapOffsetX(), getMapOffsetY());

			drawConnection(
					g2,
					"Library",
					"Student Center"
			);

			drawConnection(
					g2,
					"Library",
					"Science Building"
			);

			drawConnection(
					g2,
					"Student Center",
					"Engineering Hall"
			);

			drawConnection(
					g2,
					"Student Center",
					"Recreation Center"
			);

			drawConnection(
					g2,
					"Science Building",
					"Engineering Hall"
			);

			drawConnection(
					g2,
					"Engineering Hall",
					"Recreation Center"
			);

			drawConnection(
				g2,
				"Science Building",
				"Recreation Center"
			);

			for (String location : positions.keySet()) {
				drawBuilding(
						g2,
						location,
						positions.get(location)
				);
			}
			g2.dispose();
		}


		private void drawConnection(
				Graphics2D g2,
				String first,
				String second) {
			Point p1 = positions.get(first);
			Point p2 = positions.get(second);
			boolean selected =
					routeContainsConnection(
								first,
								second
					);
			if (selected) {
				g2.setColor(ACCENT_BLUE);

				g2.setStroke(
						new BasicStroke(6)
				);

			} else {
				g2.setColor(
						new Color(71, 85, 105)
				);

				g2.setStroke(
						new BasicStroke(3)
				);
			}

			g2.drawLine(
				p1.x,
				p1.y,
				p2.x,
				p2.y
			);
		}
		private boolean routeContainsConnection(
					String first,
					String second) {
				for (int i = 0;
					i < currentRoute.size() - 1;
					i++) {

					String a =
							currentRoute.get(i);
					String b =
							currentRoute.get(i + 1);
					if ((a.equals(first) &&
							b.equals(second))
							||
							(a.equals(second) &&
									b.equals(first))) {
						return true;
					}
				}

				return false;
			}

			private void drawBuilding(
					Graphics2D g2,
					String name,
					Point point) {
				int width = 145;
				int height = 55;

				boolean selected = currentRoute.contains(name);
				if (selected) {
					g2.setColor(ACCENT_BLUE);
				} else if (name.equals(hoveredBuilding)) {
					g2.setColor(new Color(71, 120, 200));
				} else {
					g2.setColor(PANEL_BG);
				}

				g2.fillRoundRect(
						point.x - width / 2,
						point.y - height / 2,
						width,
						height,
						20,
						20
				);

				g2.setColor(new Color(100, 116, 139));

				g2.setStroke(
						new BasicStroke(2)
				);

				g2.drawRoundRect(
						point.x - width / 2,
						point.y - height / 2,
						width,
						height,
						20,
						20
				);

				g2.setColor(TEXT_WHITE);

				Font font =
						new Font(
								"Arial",
								Font.BOLD,
								12
						);

				g2.setFont(font);
				FontMetrics metrics =
						g2.getFontMetrics(font);
				int textWidth =
						metrics.stringWidth(name);
				g2.drawString(
						name,
						point. x - textWidth / 2,
						point.y + 5
				);

				if(!currentRoute.isEmpty()) {
					if(name.equals(currentRoute.get(0))) {
						g2.setColor(new Color(34, 197, 94));
						g2.fillRoundRect(
							point.x - 35,
							point.y - 55,
							70, 25, 10, 10
						);

						g2.setColor(Color.WHITE);
						g2.drawString(
							"START",
							point.x - 20,
							point.y - 38
						);
					}

					if(name.equals(currentRoute.get(currentRoute.size() - 1))) {
						g2.setColor(new Color(239, 68, 68));
						g2.fillRoundRect(
							point.x - 30,
							point.y - 55,
							60, 25, 10, 10
						);

						g2.setColor(Color.WHITE);
						g2.drawString(
							"END",
							point.x - 15,
							point.y - 38
						);
					}
				}

			}
		}


		public static void main(String[] args) {
			SwingUtilities.invokeLater(
					CampusRouteFinder::new
			);
		}
	}