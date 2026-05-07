/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package finalProject;

/**
 *
 * @author user
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
public class mainMenu {
    public static void main(String[] args) {
        
        JFrame frame = new JFrame("__Bus Route and Schedule Tracker__");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout()); // Organizes components in a row

        JButton busMap = new JButton("Bus Map");
        JButton busSchedual = new JButton("Bus schedule and arrival time");
        JButton busTracker = new JButton("Bus tracker and current station");
        JButton exitBtn = new JButton("Exit");
        
        

        busMap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "Accessing to (Bus Map) View-->");
            }
        });

        busSchedual.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "Accessing to (Bus Schedual) View-->");
            }
        });
        
        busTracker.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "Accessing to (Bus Tracker) View-->");
            }
        });

        exitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0); // Closes the program
            }
        });

        frame.add(busMap);
        frame.add(busSchedual);
        frame.add(busTracker);
        frame.add(exitBtn);


        JTextArea infoArea = new JTextArea(4, 20); 
        infoArea.setText("--- Done By: ---\n" +
                         "NURUL LUKMAN\n" + 
                         "TEE YUN\n" + 
                         "GOH XUAN\n" +
                         "CHOONG LIN\n" + 
                         "OMAR ELFASAKHANY\n" + 
                         "CHONG KANG");
        
        infoArea.setEditable(false); 
        infoArea.setBackground(new Color(240, 240, 240)); 
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        frame.add(infoArea, BorderLayout.SOUTH); 

        frame.setVisible(true);
            }
        }
