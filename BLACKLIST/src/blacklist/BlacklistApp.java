package blacklist;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BLACKLIST: original two-submission workflow plus batch text and image tools.
 * AI-writing analysis is deliberately labelled experimental and is NOT a reliable AI detector.
 */
public class BlacklistApp extends JFrame {
    private final JTextArea submission1 = new JTextArea(12, 36);
    private final JTextArea submission2 = new JTextArea(12, 36);
    private final JTextField studentNameA = new JTextField(12);
    private final JTextField rollA = new JTextField(10);
    private final JTextField studentNameB = new JTextField(12);
    private final JTextField rollB = new JTextField(10);
    private final JLabel resultLabel = new JLabel("Enter or upload two submissions to begin.");
    private final JComboBox<String> algorithmBox = new JComboBox<>(new String[]{"Word Overlap", "2-gram", "3-gram"});
    private static final double THRESHOLD = 70.0;
    private double lastScore;
    private String lastAlgorithm = "";
    private String lastStatus = "";
    private boolean hasCompared = false;
    private final List<File> batchTextFiles = new ArrayList<>();
    private final DefaultListModel<String> batchListModel = new DefaultListModel<>();
    private final List<File> imageFiles = new ArrayList<>();
    private final DefaultListModel<String> imageListModel = new DefaultListModel<>();

    public BlacklistApp() {
        super("BLACKLIST | Academic Integrity Review");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setSize(1180, 800);
        setLocationRelativeTo(null);
        submission1.setLineWrap(true); submission1.setWrapStyleWord(true);
        submission2.setLineWrap(true); submission2.setWrapStyleWord(true);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));
        root.setBackground(new Color(245, 247, 250));
        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.setOpaque(false);
        JLabel heading = new JLabel("BLACKLIST");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 27f));
        JLabel subtitle = new JLabel("Academic Integrity Review  •  Text similarity  •  Batch screening  •  Image analysis");
        subtitle.setForeground(new Color(88, 101, 120));
        header.add(heading, BorderLayout.NORTH); header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Single Comparison", buildSingleComparisonTab());
        tabs.addTab("Multiple Files", buildMultipleFilesTab());
        tabs.addTab("Image Analysis", buildImageTab());
        root.add(tabs, BorderLayout.CENTER);
        resultLabel.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 2));
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 13f));
        root.add(resultLabel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildSingleComparisonTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel submissions = new JPanel(new GridLayout(1, 2, 10, 0));
        submissions.add(createSubmissionPanel("SUBMISSION 1", studentNameA, rollA, submission1, true));
        submissions.add(createSubmissionPanel("SUBMISSION 2", studentNameB, rollB, submission2, false));
        panel.add(submissions, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.add(new JLabel("Algorithm:")); controls.add(algorithmBox);
        JButton compare = new JButton("Compare Submissions");
        JButton words = new JButton("Matching Words");
        JButton sentences = new JButton("Matching Sentences");
        JButton report = new JButton("Save Report");
        JButton uploadA = new JButton("Upload to Submission 1");
        JButton uploadB = new JButton("Upload to Submission 2");
        JButton ai = new JButton("Writing Pattern Analysis");
        JButton clear = new JButton("Clear Pair");
        compare.addActionListener(e -> compare()); words.addActionListener(e -> showMatchingWords());
        sentences.addActionListener(e -> showMatchingSentences()); report.addActionListener(e -> saveSimilarityReport());
        uploadA.addActionListener(e -> uploadInto(submission1)); uploadB.addActionListener(e -> uploadInto(submission2));
        ai.addActionListener(e -> experimentalWritingCheck()); clear.addActionListener(e -> clearPair());
        controls.add(compare); controls.add(words); controls.add(sentences); controls.add(report);
        controls.add(uploadA); controls.add(uploadB); controls.add(ai); controls.add(clear);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildMultipleFilesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
        JLabel intro = new JLabel("Add any number of student .txt submissions. Each file keeps its real filename — no Student A / Student B labels.");
        intro.setFont(intro.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(intro, BorderLayout.NORTH);
        JList<String> fileList = new JList<>(batchListModel);
        fileList.setVisibleRowCount(12);
        fileList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton add = new JButton("Add Text Files…");
        JButton remove = new JButton("Remove Selected");
        JButton compare = new JButton("Compare All Files");
        JButton ai = new JButton("Analyze Writing Patterns");
        JButton clear = new JButton("Clear File List");
        add.addActionListener(e -> addBatchFiles());
        remove.addActionListener(e -> {
            int[] selected = fileList.getSelectedIndices();
            for (int i = selected.length - 1; i >= 0; i--) { batchTextFiles.remove(selected[i]); batchListModel.remove(selected[i]); }
            resultLabel.setText(batchTextFiles.size() + " text file(s) in batch.");
        });
        compare.addActionListener(e -> compareBatchFiles());
        ai.addActionListener(e -> experimentalWritingCheck());
        clear.addActionListener(e -> { batchTextFiles.clear(); batchListModel.clear(); resultLabel.setText("Multiple-file list cleared."); });
        actions.add(add); actions.add(remove); actions.add(compare); actions.add(ai); actions.add(clear);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildImageTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
        JLabel intro = new JLabel("Select multiple images to compare visual similarity or inspect image properties.");
        intro.setFont(intro.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(intro, BorderLayout.NORTH);
        JList<String> list = new JList<>(imageListModel);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel imageCenter = new JPanel(new BorderLayout(8, 8));
        imageCenter.add(new JScrollPane(list), BorderLayout.CENTER);
        JTextArea note = new JTextArea("Important: image similarity is not proof of copying. AI-generated-image detection requires a trained model or external detection service; this offline version reports image properties and returns INCONCLUSIVE rather than guessing.");
        note.setLineWrap(true); note.setWrapStyleWord(true); note.setEditable(false); note.setOpaque(false); note.setRows(2);
        imageCenter.add(note, BorderLayout.SOUTH);
        panel.add(imageCenter, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton select = new JButton("Add Images…");
        JButton compare = new JButton("Compare Image Pairs");
        JButton clear = new JButton("Clear Images");
        select.addActionListener(e -> selectImages()); compare.addActionListener(e -> compareSelectedImages());
        clear.addActionListener(e -> { imageFiles.clear(); imageListModel.clear(); resultLabel.setText("Image list cleared."); });
        actions.add(select); actions.add(compare); actions.add(clear);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSubmissionPanel(String title, JTextField name, JTextField roll,
                                         JTextArea area, boolean first) {
        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JPanel details = new JPanel(new GridLayout(2,2,5,4));
        details.add(new JLabel("Student name:")); details.add(name);
        details.add(new JLabel("Roll number:")); details.add(roll);
        panel.add(details, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private void uploadInto(JTextArea target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a text file");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            target.setText(Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8));
            resultLabel.setText("Loaded: " + chooser.getSelectedFile().getName());
        } catch (IOException ex) {
            showError("Could not read file: " + ex.getMessage());
        }
    }

    private void compare() {
        String a = submission1.getText(), b = submission2.getText();
        if (a.isBlank() || b.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter or upload both submissions.", "Missing Text", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String algorithm = (String)algorithmBox.getSelectedItem();
        lastScore = calculate(a,b,algorithm);
        lastAlgorithm = algorithm;
        lastStatus = lastScore >= THRESHOLD ? "FLAGGED - Teacher should review" : "Below review threshold";
        hasCompared = true;
        resultLabel.setText(String.format(Locale.ROOT, "Similarity: %.2f%% | %s | Threshold: %.0f%%", lastScore,lastStatus,THRESHOLD));
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+"," ").trim();
    }
    private static List<String> tokens(String text) {
        String n = normalize(text);
        return n.isEmpty() ? new ArrayList<>() : Arrays.asList(n.split(" "));
    }
    private static double calculate(String a, String b, String algorithm) {
        List<String> ta=tokens(a), tb=tokens(b);
        if ("Word Overlap".equals(algorithm)) {
            Set<String> sa=new HashSet<>(ta), sb=new HashSet<>(tb);
            if (sa.isEmpty() && sb.isEmpty()) return 0;
            Set<String> common=new HashSet<>(sa); common.retainAll(sb);
            return 200.0*common.size()/(sa.size()+sb.size());
        }
        int n = "3-gram".equals(algorithm) ? 3 : 2;
        Set<String> ga=ngrams(ta,n), gb=ngrams(tb,n);
        if (ga.isEmpty() && gb.isEmpty()) return 0;
        Set<String> common=new HashSet<>(ga); common.retainAll(gb);
        return 200.0*common.size()/(ga.size()+gb.size());
    }
    private static Set<String> ngrams(List<String> tokens, int n) {
        Set<String> result=new HashSet<>();
        for(int i=0;i+n<=tokens.size();i++) result.add(String.join(" ",tokens.subList(i,i+n)));
        return result;
    }

    private void showMatchingWords() {
        String a=submission1.getText(), b=submission2.getText();
        if(a.isBlank()||b.isBlank()){ missingPair(); return; }
        Set<String> wordsB=new HashSet<>(tokens(b));
        JTextPane pane=new JTextPane(); pane.setEditable(false);
        StyledDocument doc=pane.getStyledDocument();
        Style normal=pane.addStyle("normal",null); StyleConstants.setForeground(normal,Color.BLACK);
        Style match=pane.addStyle("match",null); StyleConstants.setBackground(match,Color.YELLOW); StyleConstants.setBold(match,true);
        Matcher m=Pattern.compile("[a-zA-Z0-9]+|[^a-zA-Z0-9]+").matcher(a);
        int count=0;
        try {
            while(m.find()){
                String token=m.group(); boolean isWord=token.matches("[a-zA-Z0-9]+");
                boolean common=isWord&&wordsB.contains(token.toLowerCase(Locale.ROOT));
                if(common)count++;
                doc.insertString(doc.getLength(),token,common?match:normal);
            }
        } catch(BadLocationException ex){showError("Could not display matching words.");return;}
        showResultWindow("BLACKLIST | Matching Words",pane,"Yellow = word also appears in Submission B | Matching occurrences: "+count,750,500);
    }

    private void showMatchingSentences() {
        String a=submission1.getText(), b=submission2.getText();
        if(a.isBlank()||b.isBlank()){missingPair();return;}
        Set<String> sentencesB=new HashSet<>();
        for(String s:b.split("(?<=[.!?])\\s+")) if(!s.trim().isEmpty()) sentencesB.add(normalize(s));
        JTextPane pane=new JTextPane(); pane.setEditable(false);
        StyledDocument doc=pane.getStyledDocument();
        Style normal=pane.addStyle("normal",null); StyleConstants.setForeground(normal,Color.BLACK);
        Style match=pane.addStyle("match",null); StyleConstants.setBackground(match,Color.YELLOW); StyleConstants.setBold(match,true);
        int count=0;
        try{
            for(String s:a.split("(?<=[.!?])\\s+")){
                String clean=s.trim(); if(clean.isEmpty())continue;
                boolean common=sentencesB.contains(normalize(clean));
                doc.insertString(doc.getLength(),clean+"\n\n",common?match:normal);
                if(common)count++;
            }
        }catch(BadLocationException ex){showError("Could not display matching sentences.");return;}
        if(count==0)pane.setText("No exact matching sentences found in Submission A.");
        showResultWindow("BLACKLIST | Matching Sentences",pane,"Yellow = exact matching sentence | Matches: "+count,700,500);
    }

    private void showResultWindow(String title, JTextPane pane, String message, int w, int h) {
        JFrame frame=new JFrame(title); frame.add(new JScrollPane(pane),BorderLayout.CENTER);
        JLabel info=new JLabel(message); info.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        frame.add(info,BorderLayout.SOUTH); frame.setSize(w,h); frame.setLocationRelativeTo(this);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); frame.setVisible(true); frame.toFront(); frame.requestFocus();
    }

    private void saveSimilarityReport() {
        if(!hasCompared){JOptionPane.showMessageDialog(this,"Please compare both submissions before saving a report.","No Comparison",JOptionPane.WARNING_MESSAGE);return;}
        JFileChooser chooser=new JFileChooser(); chooser.setDialogTitle("Save Similarity Report");
        chooser.setSelectedFile(new File("BLACKLIST_Similarity_Report.txt"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        File file=chooser.getSelectedFile();
        if(!file.getName().toLowerCase(Locale.ROOT).endsWith(".txt"))file=new File(file.getAbsolutePath()+".txt");
        String date=LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String report="========================================\n        BLACKLIST SIMILARITY REPORT\n========================================\n\n"+
            "Report Date and Time: "+date+"\n\nSTUDENT DETAILS\nStudent A Name: "+studentNameA.getText().trim()+
            "\nStudent A Roll Number: "+rollA.getText().trim()+"\nStudent B Name: "+studentNameB.getText().trim()+
            "\nStudent B Roll Number: "+rollB.getText().trim()+"\n\nAlgorithm Used: "+lastAlgorithm+
            String.format(Locale.ROOT,"\nSimilarity Score: %.2f%%\n",lastScore)+"Review Status: "+lastStatus+
            String.format(Locale.ROOT,"\nReview Threshold: %.2f%%\n\n",THRESHOLD)+"SUBMISSION A\n"+submission1.getText()+
            "\n\nSUBMISSION B\n"+submission2.getText()+"\n\nNote: Score is an automated estimate. Teacher review is required.\n";
        try{Files.writeString(file.toPath(),report,StandardCharsets.UTF_8);JOptionPane.showMessageDialog(this,"Report saved:\n"+file.getAbsolutePath());}
        catch(IOException ex){showError("Could not save report: "+ex.getMessage());}
    }

    private void addBatchFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Add student submission text files");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        int added = 0;
        for (File file : chooser.getSelectedFiles()) {
            boolean duplicate = batchTextFiles.stream().anyMatch(existing -> existing.getAbsolutePath().equals(file.getAbsolutePath()));
            if (!duplicate) { batchTextFiles.add(file); batchListModel.addElement(file.getName() + "   —   " + file.getAbsolutePath()); added++; }
        }
        resultLabel.setText("Added " + added + " file(s). Total submissions: " + batchTextFiles.size() + ".");
    }

    private void clearPair() {
        submission1.setText(""); submission2.setText(""); studentNameA.setText(""); studentNameB.setText("");
        rollA.setText(""); rollB.setText(""); lastScore=0; lastAlgorithm=""; lastStatus=""; hasCompared=false;
        resultLabel.setText("Single comparison cleared.");
    }

    private void compareBatchFiles() {
        if (batchTextFiles.isEmpty()) {
            addBatchFiles();
            if (batchTextFiles.isEmpty()) return;
        }
        File[] files = batchTextFiles.toArray(new File[0]);
        if (files.length < 2) {
            JOptionPane.showMessageDialog(this, "Select at least two .txt files for batch comparison.",
                "More files required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] columns = {"Submission file 1", "Submission file 2", "Similarity", "Review status"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        int flagged = 0;
        for (int i = 0; i < files.length; i++) {
            for (int j = i + 1; j < files.length; j++) {
                try {
                    String text1 = Files.readString(files[i].toPath(), StandardCharsets.UTF_8);
                    String text2 = Files.readString(files[j].toPath(), StandardCharsets.UTF_8);
                    double score = calculate(text1, text2, (String) algorithmBox.getSelectedItem());
                    boolean review = score >= THRESHOLD;
                    if (review) flagged++;
                    model.addRow(new Object[]{files[i].getName(), files[j].getName(),
                        String.format(Locale.ROOT, "%.2f%%", score),
                        review ? "FLAG FOR TEACHER REVIEW" : "Below threshold"});
                } catch (IOException ex) {
                    model.addRow(new Object[]{files[i].getName(), files[j].getName(), "N/A", "Read error: " + ex.getMessage()});
                }
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(210);
        table.getColumnModel().getColumn(1).setPreferredWidth(210);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(190);

        JFrame frame = new JFrame("BLACKLIST | Batch Comparison Results");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        JLabel summary = new JLabel("  " + files.length + " files  ·  " + model.getRowCount() +
            " pairwise comparisons  ·  " + flagged + " flagged for teacher review  ·  Threshold: " + THRESHOLD + "%");
        summary.setBorder(BorderFactory.createEmptyBorder(10, 6, 4, 6));
        summary.setFont(summary.getFont().deriveFont(Font.BOLD, 14f));
        frame.add(summary, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        JLabel note = new JLabel("  Each submission is identified by its actual file name. A flagged score is a review signal, not proof of plagiarism.");
        note.setBorder(BorderFactory.createEmptyBorder(4, 6, 10, 6));
        frame.add(note, BorderLayout.SOUTH);
        frame.setSize(920, 500);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
        frame.toFront();
        resultLabel.setText("Batch complete: " + files.length + " files, " + model.getRowCount() + " pairwise comparisons.");
    }

    // This transparent heuristic is not a trained AI detector and must never be used as proof.
    private void experimentalWritingCheck() {
        List<File> files=new ArrayList<>(batchTextFiles);
        if(files.isEmpty()){
            JFileChooser chooser=new JFileChooser();chooser.setMultiSelectionEnabled(true);
            chooser.setDialogTitle("Select text file(s) for experimental writing-pattern check");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)","txt"));
            if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
            files.addAll(Arrays.asList(chooser.getSelectedFiles()));
        }
        StringBuilder out=new StringBuilder("EXPERIMENTAL WRITING-PATTERN INDICATOR\nNOT A RELIABLE AI DETECTOR — do not treat as proof of AI authorship.\n\n");
        for(File f:files)try{
            String text=Files.readString(f.toPath(),StandardCharsets.UTF_8).trim();
            List<String> tok=tokens(text);
            String[] sentences=text.isEmpty()?new String[0]:text.split("(?<=[.!?])\\s+");
            double avg=sentences.length==0?0:(double)tok.size()/sentences.length;
            out.append(f.getName()).append("\nWords: ").append(tok.size())
               .append(String.format(Locale.ROOT,"\nAverage words per sentence: %.1f\nAI Detection: ",avg))
               .append(tok.size()<80
                   ? "INCONCLUSIVE — insufficient text for analysis\n\n"
                   : "INCONCLUSIVE — this writing-pattern check cannot reliably determine AI authorship\n\n");
        }catch(IOException ex){out.append(f.getName()).append(": ").append(ex.getMessage()).append("\n\n");}
        showTextWindow("BLACKLIST | Experimental Writing Analysis",out.toString());
    }

    private void selectImages() {
        JFileChooser chooser=new JFileChooser();chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select multiple images");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images","png","jpg","jpeg","bmp","gif"));
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        int added = 0;
        for (File file : chooser.getSelectedFiles()) {
            boolean duplicate = imageFiles.stream().anyMatch(existing -> existing.getAbsolutePath().equals(file.getAbsolutePath()));
            if (!duplicate) { imageFiles.add(file); imageListModel.addElement(file.getName() + "   —   " + file.getAbsolutePath()); added++; }
        }
        resultLabel.setText("Added " + added + " image(s). Total images: " + imageFiles.size() + ".");
    }

    private void compareSelectedImages() {
        if(imageFiles.size()<2){JOptionPane.showMessageDialog(this,"First click Select Images and choose at least two images.");return;}
        StringBuilder out=new StringBuilder("BASIC IMAGE SIMILARITY ESTIMATES\nResized grayscale pixel comparison; not an image-authenticity detector.\n\n");
        for(int i=0;i<imageFiles.size();i++)for(int j=i+1;j<imageFiles.size();j++){
            File a=imageFiles.get(i),b=imageFiles.get(j);
            try{
                BufferedImage ia=ImageIO.read(a),ib=ImageIO.read(b);
                if(ia==null||ib==null){out.append(a.getName()).append(" vs ").append(b.getName()).append(": unsupported image.\n\n");continue;}
                double score=imageScore(ia,ib);
                out.append(a.getName()).append(" vs ").append(b.getName())
                    .append(String.format(Locale.ROOT,"\nVisual similarity estimate: %.2f%%\n%s\n\n",score,score>=90?"Very similar pixel patterns; inspect manually":"Review manually if needed"));
            }catch(IOException ex){out.append("Error: ").append(ex.getMessage()).append("\n\n");}
        }
        showTextWindow("BLACKLIST | Image Comparison",out.toString());
    }
    /** Runs the configured pretrained image classifier through the bundled Python bridge. */
    private void detectSelectedImagesWithModel() {
        if (imageFiles.isEmpty()) {
            selectImages();
            if (imageFiles.isEmpty()) return;
        }
        File script = locateImageDetectorScript();
        if (script == null || !script.isFile()) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Locate ai_image_detector.py (inside the extracted BLACKLIST folder)");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setSelectedFile(new File("ai_image_detector.py"));
            int choice = chooser.showOpenDialog(this);
            if (choice != JFileChooser.APPROVE_OPTION) return;
            script = chooser.getSelectedFile();
            try { script = script.getCanonicalFile(); } catch (IOException ignored) { }
            if (!script.isFile()) {
                JOptionPane.showMessageDialog(this, "That file does not exist. Select ai_image_detector.py.", "AI Detector Setup", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        try { script = script.getCanonicalFile(); } catch (IOException ignored) { }
        List<String> command = new ArrayList<>();
        // Look for the virtual environment beside the project, then in the user's home folder.
        File venvPython = new File(script.getParentFile(), ".venv\\Scripts\\python.exe");
        File homeVenvPython = new File(System.getProperty("user.home"), ".venv\\Scripts\\python.exe");
        if (venvPython.isFile()) {
            command.add(venvPython.getAbsolutePath());
        } else if (homeVenvPython.isFile()) {
            command.add(homeVenvPython.getAbsolutePath());
        } else {
            command.add("python");
        }
        final File detectorScript = script;
        command.add(detectorScript.getAbsolutePath());
        for (File image : imageFiles) command.add(image.getAbsolutePath());
        resultLabel.setText("AI image detection running for " + imageFiles.size() + " image(s)…");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                ProcessBuilder pb = new ProcessBuilder(command);
                // Use an existing absolute working directory. A stale/relative folder such as
                // "Gallery" causes Windows CreateProcess error 267.
                File workingDir = detectorScript.getCanonicalFile().getParentFile();
                if (workingDir == null || !workingDir.isDirectory()) {
                    workingDir = new File(System.getProperty("user.dir")).getCanonicalFile();
                }
                if (workingDir == null || !workingDir.isDirectory()) {
                    workingDir = new File(System.getProperty("user.home")).getCanonicalFile();
                }
                if (workingDir == null || !workingDir.isDirectory()) {
                    throw new IOException("Could not find a valid working folder for the AI detector.");
                }
                pb.directory(workingDir);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int exit = process.waitFor();
                if (exit != 0) throw new IOException(output.isBlank() ? "Python detector failed (exit " + exit + ")." : output);
                return output;
            }
            @Override protected void done() {
                try {
                    showTextWindow("BLACKLIST | AI Image Detector Results",
                        "PRETRAINED AI-IMAGE CLASSIFIER\nPredictions are estimates, not proof. Model errors and false positives are possible.\n\n" + get());
                    resultLabel.setText("AI image analysis complete for " + imageFiles.size() + " image(s).");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showError("AI image detection failed. Check that Python, transformers, torch, Pillow, and the model are installed.\n\n" + cause.getMessage());
                    resultLabel.setText("AI image detection failed.");
                }
            }
        }.execute();
    }

    private File locateImageDetectorScript() {
        File current = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 5 && current != null; i++, current = current.getParentFile()) {
            File candidate = new File(current, "ai_image_detector.py");
            if (candidate.isFile()) return candidate;
        }
        try {
            File codeLocation = new File(BlacklistApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File base = codeLocation.isDirectory() ? codeLocation : codeLocation.getParentFile();
            for (int i = 0; i < 5 && base != null; i++, base = base.getParentFile()) {
                File candidate = new File(base, "ai_image_detector.py");
                if (candidate.isFile()) return candidate;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static double imageScore(BufferedImage a,BufferedImage b){
        int w=32,h=32;double diff=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int ca=gray(a.getRGB(x*a.getWidth()/w,y*a.getHeight()/h));
            int cb=gray(b.getRGB(x*b.getWidth()/w,y*b.getHeight()/h));
            diff+=Math.abs(ca-cb)/255.0;
        }
        return Math.max(0,(1-diff/(w*h))*100);
    }
    private static int gray(int rgb){return (int)(.299*((rgb>>16)&255)+.587*((rgb>>8)&255)+.114*(rgb&255));}

    private void showTextWindow(String title,String text){
        JTextArea area=new JTextArea(text,24,90);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);
        JFrame frame=new JFrame(title);frame.add(new JScrollPane(area));frame.setSize(850,550);frame.setLocationRelativeTo(this);frame.setVisible(true);frame.toFront();frame.requestFocus();
    }
    private void clearAll(){
        submission1.setText("");submission2.setText("");studentNameA.setText("");studentNameB.setText("");
        rollA.setText("");rollB.setText("");lastScore=0;lastAlgorithm="";lastStatus="";hasCompared=false;
        batchTextFiles.clear();batchListModel.clear();imageFiles.clear();imageListModel.clear();resultLabel.setText("Ready. Choose a tab to compare submissions, multiple files, or images.");
    }
    private void missingPair(){JOptionPane.showMessageDialog(this,"Please enter or upload both submissions.","Missing Text",JOptionPane.WARNING_MESSAGE);}
    private void showError(String message){JOptionPane.showMessageDialog(this,message,"BLACKLIST Error",JOptionPane.ERROR_MESSAGE);}

    public static void main(String[] args){SwingUtilities.invokeLater(()->new BlacklistApp().setVisible(true));}
}
