package hexhelp;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Properties;

/**
 * Created by hendrik on 27.09.15.
 */
public class Main implements Runnable {

    public static final String version;
    public static final String buildDate;

    static {
        InputStream is = Main.class.getResourceAsStream("/build.properties");
        String tempVersion = "<unknown>";
        String tempBuildDate = "<unknown>";
        if(is!=null) {
            Properties p = new Properties();
            try {
                p.load(is);

                tempVersion = p.getProperty("version", "<unknown>");
                tempBuildDate = p.getProperty("buildDate", "<unknown>");
            } catch (IOException e) {
            }
        }
        version = tempVersion;
        buildDate = tempBuildDate;
    }

    public static final String sMainInfoPattern = "  %1$d %1$016X\nR %2$d %2$016X\n";
    public static final String sInfoPattern = "%s%s %s\n";
    public static final String sInfoPatternCLI = "%016X %s%s %s\n";

    private JLabel pMainInfo;
    private JLabel pLEInfo;
    private JLabel pBEInfo;
    private JLabel pRLEInfo;
    private JLabel pRBEInfo;

    private JEditorPane epInfo;

    private JTextField tfInput;
    private Color fgColor;

    private static byte [] data = new byte [8];
    private static ByteBuffer bbData = ByteBuffer.wrap(data);
    private static StringBuilder sbData = new StringBuilder();

    private static String byteString(long value, boolean littleEndian) {
        bbData.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        bbData.putLong(0, value);

        sbData.setLength(0);
        for(int i=0; i<8; i++) {
            if(i>0) {
                sbData.append(" ");
            }
            sbData.append(String.format("%02X", data[i]));
        }

        return sbData.toString();
    }

    private void update(long value) {
        long reverseValue = Long.reverse(value);

        epInfo.setText(
                String.format(
                        "<html><pre>%s%s%s%s%s</pre></html>"
                        , String.format(sMainInfoPattern, value, reverseValue)
                        , String.format(sInfoPattern, "  ", "LE", byteString(value, true))
                        , String.format(sInfoPattern, "  ", "BE", byteString(value, false))
                        , String.format(sInfoPattern, "R ", "LE", byteString(reverseValue, true))
                        , String.format(sInfoPattern, "R ", "BE", byteString(reverseValue, false))
                )
        );
    }

    private class ValueUpdater implements Runnable {
        @Override
        public void run() {
            String s = tfInput.getText().trim();
            if(s.length()<1) {
                update(0L);
                return;
            }
            tfInput.setForeground(fgColor);
            long v;
            try {
                if (s.startsWith("0x")) {
                    if(s.length()==2) {
                        // do not mark red when users starts typing
                        return;
                    }
                    v = Long.parseLong(s.substring(2), 16);
                } else {
                    v = Long.parseLong(s);
                }
                tfInput.setForeground(fgColor);
                update(v);
            } catch(NumberFormatException nfe) {
                tfInput.setForeground(Color.red);
                return;
            }
        }
    }

    private ValueUpdater valueUpdater = new ValueUpdater();

    private KeyListener inputListener = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {
            // we can't do the update here because sometimes the text is not yet
            // updated in the text field
            SwingUtilities.invokeLater(valueUpdater);
        }

        @Override
        public void keyPressed(KeyEvent e) {

        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    };

    public void run() {
        JFrame f = new JFrame("hexhelp v" + version, MouseInfo.getPointerInfo().getDevice().getDefaultConfiguration());
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f.setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
        infoPanel.setBackground(Color.white);

        infoPanel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        infoPanel.add(epInfo = new JEditorPane());
        epInfo.setEditorKit(new HTMLEditorKit());
        epInfo.setEditable(false);

        f.add(infoPanel, BorderLayout.CENTER);
        f.add(tfInput = new JTextField(), BorderLayout.SOUTH);
        fgColor = tfInput.getForeground();
        if(fgColor == null) {
            fgColor = Color.black;
        }
        tfInput.addKeyListener(inputListener);

        update(1L);

        f.addWindowListener(windowAdapter);
        f.pack();
        f.setVisible(true);
    }

    private WindowAdapter windowAdapter = new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
            tfInput.requestFocus();
        }
    };

    private static <A> boolean contains(A item, A...list) {
        for(A i : list) {
            if(item.equals(i)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String...args) {
        if((System.console() == null && args.length == 0) || contains("--gui", args)) {
            SwingUtilities.invokeLater(new Main());
            return;
        }

        if(args.length==0) {
            System.out.format("hexhelper v%s - %s\n", version, buildDate);
            System.out.format("Run with '--gui' switch for graphical interface\nor with numbers to display in console\nEnter hex-numbers with 0x prefix\n");
            return;
        }

        for(String s : args) {
            Long l;
            if(s.startsWith("0x")) {
                l = Long.parseLong(s.substring(2), 16);
            } else {
                l = Long.parseLong(s);
            }
            long rl = Long.reverse(l);
            System.out.format(sMainInfoPattern, l, rl);
            System.out.format(sInfoPatternCLI, l, "  ", "LE", byteString(l, true));
            System.out.format(sInfoPatternCLI, l, "  ", "BE", byteString(l, false));
            System.out.format(sInfoPatternCLI, rl, "R ", "LE", byteString(rl, true));
            System.out.format(sInfoPatternCLI, rl, "R ", "BE", byteString(rl, false));

        }
    }
}
