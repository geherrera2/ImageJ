package ij;

import ij.text.*;
import ij.io.*;
import ij.util.Tools;
import java.awt.*;

/** This class consists of static utility methods. */
public class IJLog {
	private static ImageJ ij;
	private static TextPanel logPanel;

	public static synchronized void log(String s) {
		if (s == null)
			return;
		if (logPanel == null && ij != null) {
			TextWindow logWindow = new TextWindow("Log", "", 400, 250);
			logPanel = logWindow.getTextPanel();
			logPanel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		}
		if (logPanel != null) {
			if (s.startsWith("\\"))
				handleLogCommand(s);
			else {
				if (s.endsWith("\n")) {
					if (s.equals("\n\n"))
						s = "\n \n ";
					else if (s.endsWith("\n\n"))
						s = s.substring(0, s.length() - 2) + "\n \n ";
					else
						s = s + " ";
				}
				logPanel.append(s);
			}
		} else {
			LogStream.redirectSystem(false);
			System.out.println(s);
		}
	}

	static void handleLogCommand(String s) {
		if (s.equals("\\Closed"))
			logPanel = null;
		else if (s.startsWith("\\Update:")) {
			int n = logPanel.getLineCount();
			String s2 = s.substring(8, s.length());
			if (n == 0)
				logPanel.append(s2);
			else
				logPanel.setLine(n - 1, s2);
		} else if (s.startsWith("\\Update")) {
			int cindex = s.indexOf(":");
			if (cindex == -1) {
				logPanel.append(s);
				return;
			}
			String nstr = s.substring(7, cindex);
			int line = (int) Tools.parseDouble(nstr, -1);
			if (line < 0 || line > 25) {
				logPanel.append(s);
				return;
			}
			int count = logPanel.getLineCount();
			while (line >= count) {
				log("");
				count++;
			}
			String s2 = s.substring(cindex + 1, s.length());
			logPanel.setLine(line, s2);
		} else if (s.equals("\\Clear")) {
			logPanel.clear();
		} else if (s.startsWith("\\Heading:")) {
			logPanel.updateColumnHeadings(s.substring(10));
		} else if (s.equals("\\Close")) {
			Frame f = WindowManager.getFrame("Log");
			if (f != null && (f instanceof TextWindow))
				((TextWindow) f).close();
		} else
			logPanel.append(s);
	}

}
