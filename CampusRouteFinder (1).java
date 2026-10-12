import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CampusRouteFinder extends JFrame {
    static class Edge {
        String to;
        double miles;
        Edge(String to, double miles) { this.to = to; this.miles = miles; }
    }
    static class Stop implements Comparable<Stop> {
        String name;
        double distance;
        Stop(String name, double distance) { this.name = name; this.distance = distance; }
        public int compareTo(Stop other) { return Double.compare(distance, other.distance); }
    }

    private final String[] names = {"Library", "Student Center", "Engineering Hall", "Science Building", "Recreation Center"};
    private final Map<String, List<Edge>> graph = new LinkedHashMap<>();
    private final Map<String, Point> points = new LinkedHashMap<>();
    private final JComboBox<String> fromBox = new JComboBox<>(names);
    private final JComboBox<String> toBox = new JComboBox<>(names);
    private final JLabel routeText = new JLabel("Choose two buildings to find a route.");
    private final JLabel statsText = new JLabel("Distance: --     Walking time: --");
    private List<String> highlighted = new ArrayList<>();
    private final JPanel map = new JPanel() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double sx = getWidth() / 700.0;
            double sy = getHeight() / 480.0;
            g.scale(sx, sy);
            for (String from : graph.keySet()) {
                for (Edge edge : graph.get(from)) {
                    if (from.compareTo(edge.to) >= 0) continue;
                    Point a = points.get(from), b = points.get(edge.to);
                    boolean active = isRouteEdge(from, edge.to);
                    g.setColor(active ? new Color(37, 99, 235) : new Color(170, 183, 202));
                    g.setStroke(new BasicStroke(active ? 7 : 3));
                    g.drawLine(a.x, a.y, b.x, b.y);
                }
            }
            for (String name : names) {
                Point p = points.get(name);
                boolean active = highlighted.contains(name);
                g.setColor(active ? new Color(37, 99, 235) : Color.WHITE);
                g.fillRoundRect(p.x - 88, p.y - 25, 176, 50, 18, 18);
                g.setColor(new Color(30, 41, 59));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(p.x - 88, p.y - 25, 176, 50, 18, 18);
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.setColor(active ? Color.WHITE : new Color(30, 41, 59));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(name, p.x - fm.stringWidth(name) / 2, p.y + 5);
            }
            g.dispose();
        }
    };

    public CampusRouteFinder() {
        for (String name : names) graph.put(name, new ArrayList<>());
        connect("Library", "Student Center", 0.3);
        connect("Library", "Science Building", 0.5);
        connect("Student Center", "Engineering Hall", 0.4);
        connect("Student Center", "Recreation Center", 0.6);
        connect("Science Building", "Engineering Hall", 0.3);
        connect("Science Building", "Recreation Center", 0.5);
        connect("Engineering Hall", "Recreation Center", 0.4);

        points.put("Library", new Point(125, 110));
        points.put("Student Center", new Point(370, 90));
        points.put("Engineering Hall", new Point(570, 225));
        points.put("Science Building", new Point(215, 340));
        points.put("Recreation Center", new Point(500, 400));

        setTitle("Campus Route Finder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("CAMPUS ROUTE FINDER", SwingConstants.CENTER);
        title.setOpaque(true);
        title.setBackground(new Color(30, 41, 59));
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setBorder(new EmptyBorder(20, 10, 20, 10));
        add(title, BorderLayout.NORTH);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setPreferredSize(new Dimension(235, 0));
        controls.setBorder(new EmptyBorder(25, 15, 15, 15));
        controls.add(new JLabel("Starting location"));
        controls.add(Box.createVerticalStrut(8));
        fromBox.setMaximumSize(new Dimension(210, 35));
        controls.add(fromBox);
        controls.add(Box.createVerticalStrut(25));
        controls.add(new JLabel("Destination"));
        controls.add(Box.createVerticalStrut(8));
        toBox.setMaximumSize(new Dimension(210, 35));
        controls.add(toBox);
        controls.add(Box.createVerticalStrut(25));
        JButton find = new JButton("Find Shortest Route");
        find.setMaximumSize(new Dimension(210, 42));
        find.addActionListener(e -> findRoute());
        controls.add(find);
        controls.add(Box.createVerticalStrut(10));
        JButton reset = new JButton("Reset");
        reset.setMaximumSize(new Dimension(210, 38));
        reset.addActionListener(e -> {
            fromBox.setSelectedIndex(0);
            toBox.setSelectedIndex(0);
            highlighted.clear();
            routeText.setText("Choose two buildings to find a route.");
            statsText.setText("Distance: --     Walking time: --");
            map.repaint();
        });
        controls.add(reset);
        add(controls, BorderLayout.WEST);

        map.setBackground(new Color(241, 245, 249));
        add(map, BorderLayout.CENTER);

        JPanel results = new JPanel(new GridLayout(2, 1, 0, 5));
        results.setBorder(new EmptyBorder(12, 20, 15, 20));
        routeText.setFont(new Font("SansSerif", Font.BOLD, 14));
        results.add(routeText);
        results.add(statsText);
        add(results, BorderLayout.SOUTH);
        toBox.setSelectedIndex(2);
        setVisible(true);
    }

    private void connect(String a, String b, double miles) {
        graph.get(a).add(new Edge(b, miles));
        graph.get(b).add(new Edge(a, miles));
    }

    private boolean isRouteEdge(String a, String b) {
        for (int i = 0; i < highlighted.size() - 1; i++) {
            String x = highlighted.get(i), y = highlighted.get(i + 1);
            if ((x.equals(a) && y.equals(b)) || (x.equals(b) && y.equals(a))) return true;
        }
        return false;
    }

    private void findRoute() {
        String start = (String) fromBox.getSelectedItem();
        String end = (String) toBox.getSelectedItem();
        if (start.equals(end)) {
            JOptionPane.showMessageDialog(this, "Choose two different locations.");
            return;
        }
        Map<String, Double> distance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<Stop> queue = new PriorityQueue<>();
        for (String name : names) distance.put(name, Double.POSITIVE_INFINITY);
        distance.put(start, 0.0);
        queue.add(new Stop(start, 0.0));
        while (!queue.isEmpty()) {
            Stop current = queue.poll();
            if (current.distance > distance.get(current.name)) continue;
            if (current.name.equals(end)) break;
            for (Edge edge : graph.get(current.name)) {
                double candidate = current.distance + edge.miles;
                if (candidate < distance.get(edge.to)) {
                    distance.put(edge.to, candidate);
                    previous.put(edge.to, current.name);
                    queue.add(new Stop(edge.to, candidate));
                }
            }
        }
        if (Double.isInfinite(distance.get(end))) {
            JOptionPane.showMessageDialog(this, "No route found.");
            return;
        }
        highlighted = new ArrayList<>();
        for (String at = end; at != null; at = previous.get(at)) highlighted.add(at);
        Collections.reverse(highlighted);
        double miles = distance.get(end);
        routeText.setText("Route: " + String.join("  ->  ", highlighted));
        statsText.setText(String.format("Distance: %.2f miles     Walking time: %.0f minutes", miles, miles / 3.0 * 60));
        map.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CampusRouteFinder::new);
    }
}
