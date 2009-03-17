/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: MainDialog.java,v 1.4 2006/10/19 05:09:33 gojomo Exp $
 */
package org.bitpedia.collider.ui;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.bitpedia.collider.core.Bitcollider;
import org.bitpedia.collider.core.Submission;

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingExecutionException;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class MainDialog extends JPanel implements ActionListener,
		DropTargetListener, Bitcollider.Progress {

	public static final int GAP = 6;
	
	private Collection fmtHandlers;

	private JFrame frame;

	private SpringLayout layout = new SpringLayout();

	private JLabel lblCurFile = new JLabel("Current File:");

	private JProgressBar pbCurFile = new JProgressBar(0, 100);

	private JLabel lblOverallProgress = new JLabel("Overall progress:");

	private JProgressBar pbOverallProgress = new JProgressBar(0, 100);

	private JLabel lblFilesToProcess = new JLabel("0 files to process",
			SwingConstants.CENTER);

	private JLabel lblFilesProcessed = new JLabel("0 files processed",
			SwingConstants.CENTER);

	private JLabel lblFilesSkipped = new JLabel("0 files skipped",
			SwingConstants.CENTER);

	private JTextArea taHint = new JTextArea(
			" (1) Drag files onto this window\n"
					+ " (2) The Bitcollider extracts identifiers and metadata\n"
					+ " (3) Your default web browser will launch to submit the info");

	private JCheckBox chbCloseWindow = new JCheckBox(
			"Close window after processing", false);

	private JCheckBox chbDontShowForSmall = new JCheckBox(
			"Don't show window for small files", true);

	private JButton btnAbout = new JButton("About");

	private JButton btnBrowse = new JButton("Browse");

	private JButton btnClose = new JButton("Close");

	private Spring sprPnlWidth = new WidthSpring(this);
	
	private JFileChooser fileChooser = new JFileChooser();
	
	private int filesSkipped = 0;
	
	private int filesProcessed = 0;
	
	private int filesToProcess = 0;
	
	private boolean noSubmitting = false;
	
	private String alternateUrl;
	
	private boolean md5 = false;
	
	private boolean crc32 = false;

	public MainDialog(JFrame frame) {

		this.frame = frame;
		
		fileChooser.setMultiSelectionEnabled(true);

		setLayout(layout);

		add(lblCurFile);
		layout.putConstraint(SpringLayout.NORTH, lblCurFile, GAP,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, lblCurFile, GAP,
				SpringLayout.WEST, this);

		add(pbCurFile);
		layout.putConstraint(SpringLayout.EAST, pbCurFile, -GAP,
				SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.WEST, pbCurFile, GAP,
				SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, pbCurFile, GAP,
				SpringLayout.SOUTH, lblCurFile);

		add(lblOverallProgress);
		layout.putConstraint(SpringLayout.NORTH, lblOverallProgress, GAP,
				SpringLayout.SOUTH, pbCurFile);
		layout.putConstraint(SpringLayout.WEST, lblOverallProgress, GAP,
				SpringLayout.WEST, this);

		add(pbOverallProgress);
		layout.putConstraint(SpringLayout.EAST, pbOverallProgress, -GAP,
				SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.WEST, pbOverallProgress, GAP,
				SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, pbOverallProgress, GAP,
				SpringLayout.SOUTH, lblOverallProgress);

		Spring lblWidth = Spring.scale(Spring.sum(sprPnlWidth, Spring
				.constant(-4 * GAP)), 1f / 3f);

		add(lblFilesToProcess);
		layout.putConstraint(SpringLayout.NORTH, lblFilesToProcess, GAP,
				SpringLayout.SOUTH, pbOverallProgress);
		layout.putConstraint(SpringLayout.WEST, lblFilesToProcess, GAP,
				SpringLayout.WEST, this);
		layout.getConstraints(lblFilesToProcess).setWidth(lblWidth);

		add(lblFilesProcessed);
		layout.putConstraint(SpringLayout.NORTH, lblFilesProcessed, GAP,
				SpringLayout.SOUTH, pbOverallProgress);
		layout.putConstraint(SpringLayout.WEST, lblFilesProcessed, GAP,
				SpringLayout.EAST, lblFilesToProcess);
		layout.getConstraints(lblFilesProcessed).setWidth(lblWidth);

		add(lblFilesSkipped);
		layout.putConstraint(SpringLayout.EAST, lblFilesSkipped, -GAP,
				SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.WEST, lblFilesSkipped, GAP,
				SpringLayout.EAST, lblFilesProcessed);
		layout.putConstraint(SpringLayout.NORTH, lblFilesSkipped, GAP,
				SpringLayout.SOUTH, pbOverallProgress);
		layout.getConstraints(lblFilesSkipped).setWidth(lblWidth);

		add(taHint);
		taHint.setEditable(false);
		taHint.setFocusable(false);
		taHint.setBackground(this.getBackground());
		layout.putConstraint(SpringLayout.EAST, taHint, -GAP,
				SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.WEST, taHint, GAP, SpringLayout.WEST,
				this);
		layout.putConstraint(SpringLayout.NORTH, taHint, GAP,
				SpringLayout.SOUTH, lblFilesToProcess);

		add(chbCloseWindow);
		layout.putConstraint(SpringLayout.NORTH, chbCloseWindow, GAP,
				SpringLayout.SOUTH, taHint);
		layout.putConstraint(SpringLayout.WEST, chbCloseWindow, GAP,
				SpringLayout.WEST, this);

		add(chbDontShowForSmall);
		layout.putConstraint(SpringLayout.NORTH, chbDontShowForSmall, GAP,
				SpringLayout.SOUTH, chbCloseWindow);
		layout.putConstraint(SpringLayout.WEST, chbDontShowForSmall, GAP,
				SpringLayout.WEST, this);

		Spring btnWidth = Spring.scale(Spring.sum(sprPnlWidth, Spring
				.constant(-4 * GAP)), 1f / 3f);

		add(btnAbout);
		btnAbout.addActionListener(this);
		layout.putConstraint(SpringLayout.NORTH, btnAbout, GAP,
				SpringLayout.SOUTH, chbCloseWindow);
		layout.putConstraint(SpringLayout.WEST, btnAbout, GAP,
				SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, btnAbout, -GAP,
				SpringLayout.SOUTH, this);
		layout.getConstraints(btnAbout).setWidth(btnWidth);

		add(btnBrowse);
		btnBrowse.addActionListener(this);
		layout.putConstraint(SpringLayout.NORTH, btnBrowse, GAP,
				SpringLayout.SOUTH, chbCloseWindow);
		layout.putConstraint(SpringLayout.WEST, btnBrowse, GAP,
				SpringLayout.EAST, btnAbout);
		layout.putConstraint(SpringLayout.SOUTH, btnBrowse, -GAP,
				SpringLayout.SOUTH, this);
		layout.getConstraints(btnBrowse).setWidth(btnWidth);

		add(btnClose);
		btnClose.addActionListener(this);
		layout.putConstraint(SpringLayout.EAST, btnClose, -GAP,
				SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.NORTH, btnClose, GAP,
				SpringLayout.SOUTH, chbCloseWindow);
		layout.putConstraint(SpringLayout.WEST, btnClose, GAP,
				SpringLayout.EAST, btnBrowse);
		layout.putConstraint(SpringLayout.SOUTH, btnClose, -GAP,
				SpringLayout.SOUTH, this);
		layout.getConstraints(btnClose).setWidth(btnWidth);

		new DropTarget(this, this);
		new DropTarget(taHint, this);
	}
	
	public void configure(boolean noSubmitting, String alternateUrl, boolean md5, boolean crc32) {
		
		this.noSubmitting = noSubmitting; 
		this.alternateUrl = alternateUrl; 
		this.md5 = md5; 
		this.crc32 = crc32;		
	}

	public static void main(String[] args) {

		JFrame frame = new JFrame("jBitcollider");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add("Center", new MainDialog(frame));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(450, 300);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public void actionPerformed(ActionEvent event) {

		if (event.getSource() == btnAbout) {
			JOptionPane
					.showMessageDialog(
							null,
							"jBitcollider 0.1.0 \n"
									+ "(PD) 2006 The Bitzi Corporation\n\n"
									+ "For more information about the jBitcollider and Bitzi's Free\n"
									+ "Universal Media Catalog , please visit http://bitzi.com",
							"About", JOptionPane.INFORMATION_MESSAGE);
		} else if (event.getSource() == btnBrowse) {
			
			int ret = fileChooser.showOpenDialog(frame);
		    if(ret == JFileChooser.APPROVE_OPTION) {
		    	File[] selFiles = fileChooser.getSelectedFiles();
		    	List files = new LinkedList();
		    	for (int i = 0; i < selFiles.length; i++) {
		    		files.add(selFiles[i].getPath());
		    	}
		    	executeBitcollider(files); 
		    }			
		} else if (event.getSource() == btnClose) {
			System.exit(0);
		}

	}

	public void setSize(int width, int height) {
		super.setSize(width, height);

		System.out.println("setSize: " + width + ", " + height);
	}

	private void processDtde(DropTargetDragEvent dtde) {
		
		DataFlavor[] flavors = dtde.getTransferable().getTransferDataFlavors();
		for (int i = 0; i < flavors.length; i++) {
			if (flavors[i].isFlavorJavaFileListType()) {
				dtde.acceptDrag(DnDConstants.ACTION_MOVE);
				return;
			} else if (flavors[i].getMimeType().startsWith("text/uri-list")
                    && flavors[i].getRepresentationClass() == Reader.class) {
				dtde.acceptDrag(DnDConstants.ACTION_MOVE);
				return;
			} 
		}

		dtde.rejectDrag();
	}

	public void dragEnter(DropTargetDragEvent dtde) {

		processDtde(dtde);
	}

	public void dragOver(DropTargetDragEvent dtde) {

		processDtde(dtde);
	}

	public void dropActionChanged(DropTargetDragEvent dtde) {

		processDtde(dtde);
	}

	public void dragExit(DropTargetEvent dtde) {
	}
	
	private void executeBitcollider(final List files) {
		
		final Bitcollider bc = new Bitcollider(fmtHandlers);
		bc.setCalcCrc32(crc32);
		bc.setCalcMd5(md5);
		bc.setPreview(true);
		Submission prevSub = bc.generateSubmission(files, null, true);
		
		filesToProcess = prevSub.getNumBitprints();
		filesProcessed = 0;
		lblFilesToProcess.setText(""+filesToProcess+" files to process");
		
		Thread bt = new Thread(new Runnable() {

			public void run() {
				
				try {
					bc.setPreview(false);
					bc.setProgress(MainDialog.this);
					Submission submission = bc.generateSubmission(files, null,
							true);
					String tmpdir = System.getProperty("java.io.tmpdir");
					String sep = System.getProperty("file.separator");
					if ((null != tmpdir) && (!"".equals(tmpdir)) && (!tmpdir.endsWith(sep))) {
						tmpdir = tmpdir + sep;					
					}
					String htmlFileName = tmpdir + "bitprint.html";
					PrintWriter pw = new PrintWriter(htmlFileName);
					try {
						submission.setAutoSubmit(!noSubmitting);
						submission.makeHtml(pw, alternateUrl);
					} finally {
						pw.close();
					}
					
					BrowserLauncher bl = new BrowserLauncher(null);
					bl.openURLinBrowser("file://localhost/"+htmlFileName);
					
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							
							pbCurFile.setValue(0);
							pbOverallProgress.setValue(0);
							lblFilesToProcess.setText("0 files to process");
							
						}
						
					});
					
				} catch (FileNotFoundException e) {
				} catch (BrowserLaunchingInitializingException e) {
				} catch (UnsupportedOperatingSystemException e) {
				} catch (BrowserLaunchingExecutionException e) {
				}
			}
			
		});
		
		bt.start();
		
	}

	public void drop(DropTargetDropEvent dtde) {
		
		DataFlavor[] flavors = dtde.getTransferable().getTransferDataFlavors();
		Transferable tr = dtde.getTransferable();
		for (int i = 0; i < flavors.length; i++) {
			if (flavors[i].isFlavorJavaFileListType()) {
				dtde.acceptDrop(DnDConstants.ACTION_MOVE);
				try {
					List files = (List) tr.getTransferData(flavors[i]);
					executeBitcollider(files);
				} catch (UnsupportedFlavorException ufe) {
				} catch (IOException e) {
				}
				return;
			} else if (flavors[i].getMimeType().startsWith("text/uri-list")
                    && flavors[i].getRepresentationClass() == Reader.class) {
				dtde.acceptDrop(DnDConstants.ACTION_MOVE);
				try {
					BufferedReader reader = new BufferedReader((Reader)tr.getTransferData(flavors[i]));
					String line;
					List files = new ArrayList();
	                while((line = reader.readLine()) != null) {
	                    try {
                            files.add((new URI(line.trim())).getPath());
                        } catch (URISyntaxException e1) {
                            System.err.println("Skipping "+line);
                        }
	                }
					executeBitcollider(files);
				} catch (IOException e) {
				} catch (UnsupportedFlavorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
		}
		
		dtde.rejectDrop();
	}

	public void setFmtHandlers(Collection fmtHandlers) {
		this.fmtHandlers = fmtHandlers;
	}
	
	private void updateProgress(int percent, String fileName, String message) {
		
		if (0 == percent) {
			if (null != message) {
				pbCurFile.setValue(0);
				filesSkipped++;
				lblFilesSkipped.setText(""+filesSkipped+" files skipped");
			} else {
				pbCurFile.setValue(0);
			}
		} else if (percent <= 100) {
			pbCurFile.setValue(percent);
			if (null != message) {
				filesProcessed++;
				assert(filesProcessed <= filesToProcess) : "Files Processed: "+filesProcessed+" Files to Process: "+ filesToProcess;
				lblFilesProcessed.setText(""+filesProcessed+" files processed");
				pbOverallProgress.setValue(100 * filesProcessed / filesToProcess);
			}
		} 
	}

	public void progress(final int percent, final String fileName, final String message) {
		
		if (SwingUtilities.isEventDispatchThread()) {
			updateProgress(percent, fileName, message);
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					public void run() {
						updateProgress(percent, fileName, message);
					}
					
				});
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
	}

}
