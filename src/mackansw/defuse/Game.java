package mackansw.defuse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A simple click/stress game created by Mackansw as a school examination project.
 * @author Mackansw
 * @version 20.04.12
 * */

public class Game {

    private JFrame window;
    private JPanel gamePanel;
    private JPanel buttomText;
    private JPanel levelStats;
    private JLabel textureLabel;
    private JLabel fpsCounter;
    private JLabel levelCounter;
    private JLabel debugTag;
    private JLabel recordLevel;

    private ImageIcon bomb;
    private ImageIcon bomb_shrinked;
    private ImageIcon bomb_defused;
    private ImageIcon explosion;

    //The bomb fuse thread
    private Thread fuseThread;

    //Game font
    private Font gameFont = new Font(Font.MONOSPACED, Font.BOLD, 17);

    //Game foreground
    private Color gameForeground = Color.cyan;

    //Game background
    private Color gameBackground = Color.darkGray.darker();

    //Returns the games current fps
    private int fps = 0;

    //Returns the bombArcs startAngle
    private int arcStart, arcX, arcY;

    //Returns the current clicks of the player
    private int clicks = 0;

    //Returns the current level ingame
    private int currentLevel = 1;

    //The players level record
    private int levelRecord = 0;

    //The bombs bounds
    private int bombWidth = 260, bombHeight = 260, shrinkedBombWidth = 240, shrinkedBombHeight = 240;

    //The splash texts X coordinate
    private int splashX = 0;

    //Returns how many clicks the player needs to win the current level
    private int currentFinishLine = 30, lastFinishLine = 0;

    //Fuse variables
    private int fuse = 0, resetFuse = 0, lastFuse = 0;

    //Returns if the game is running in debug mode
    private boolean debugMode = false;

    //Returns if the "Clicks" key is being pressed down
    private boolean isKeyDown = false;

    //Returns if the mouse controls are enabled
    private boolean useMouse = false;

    //Returns if a level is being played
    private boolean playing = false;

    //Returns if the game loop is running
    private boolean runGameLoop = false;

    //Returns if the games start-screen is displayed
    private boolean startScreen = true;

    //Animates the splash texts when started
    private Timer textAnimation = new Timer(200, e -> {
        if(splashX == 0) {
            relocateSplashX(10);
        }
        else if(splashX == 10) {
            relocateSplashX(0);
        }
        gamePanel.repaint();
    });

    //Resets the bombs position when pressed by mouse
    private Timer mousePressed = new Timer(700, e-> {
        if(useMouse & isKeyDown) {
            releaseBomb();
            stopMousePressed();
        }
    });

    /**
     * Game constructor with enableDebugMode and fuse parameters
     * @param enableDebugMode Returns if the game should be started in debug-mode
     * @param fuse the bombs fuse
     */
    private Game(String enableDebugMode, int fuse) {

        //Checks if debugMode is enabled
        if(enableDebugMode.equals("enableDebugMode")) {
            debugMode = true;
        }

        //Sets the startValue of the fuse and saves it to a reset-fuse
        this.fuse = fuse;
        this.resetFuse = fuse;
        this.lastFuse = fuse;

        //Saves the finishLine to a reset variable
        lastFinishLine = currentFinishLine;

        //Displays game window and starts the game
        setupWindow();
        startGameLoop();
    }

    /**
     * Relocates the splash texts X coordinate on-screen
     * @param x the x coordinate on screen
     */
    private void relocateSplashX(int x) {
        this.splashX = x;
    }

    //Starts animation
    private void startAnimation() {
        this.textAnimation.start();
    }

    //Stops animation
    private void stopAnimation() {
        this.textAnimation.stop();
    }

    //Checks if animation is running
    private boolean isAnimationRunning() {
        return this.textAnimation.isRunning();
    }

    //Starts the mouse timer when called
    private void startMousePressed() {
        this.mousePressed.start();
    }

    //Stops the mouse timer when called
    private void stopMousePressed() {
        this.mousePressed.stop();
    }

    //Returns if the game is over
    private boolean isGameOver() {
        return this.fuse == 0 || clicks == currentFinishLine;
    }

    //GameLoop
    private void startGameLoop() {
        int frames = 0;
        long lastTimeChecked = System.nanoTime();

        while(runGameLoop) {

            updateGameLogic();

            //Takes a little break every loop to improve performance
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Calculates fps
            frames++;
            if(System.nanoTime() - lastTimeChecked >= 1000000000L) {
                fps = frames;
                frames = 0;
                lastTimeChecked = System.nanoTime();
                fpsCounter.setText(fps + " Fps ");
            }
        }
    }

    //Checks and updates basic game logic
    private void updateGameLogic() {
        if(playing) {
            if(this.fuse == 0) {
                playing = false;
                textureLabel.setIcon(explosion);
                recordLevel.setText(" | Level record: " + getLevelRecord(currentLevel));
                System.out.println("Game over!");
            }
            else if(clicks == currentFinishLine) {
                playing = false;
                textureLabel.setIcon(bomb_defused);
                System.out.println("Game won!");
            }
            if(isAnimationRunning()) {
                stopAnimation();
            }
        }

        //While the player is dead
        else if(isGameOver() || startScreen) {
            if(textureLabel.getIcon().getIconHeight() != bombHeight) {
                textureLabel.setIcon(bomb);
            }
            if(!isAnimationRunning()) {
                startAnimation();
            }
        }
    }

    //Starts the fuseThread and the fuse when called
    private void startFuseThread(int currentFuse) {
        playing = true;
        fuseThread = new Thread() {

            @Override
            public void run() {
                super.run();

                //Returns how often the fuse should count down by dividing the current fuse by 360 (360 = full arc)
                int fuseInterval = 360 / currentFuse;

                //Returns how fast the fuse should count down
                int sleepTime = currentFuse * 2 + 6;

                //Keeps track of when the fuse last counted down
                int fuseCheckpoint = 0;

                for(int i = 361; arcStart < i; arcStart++) {

                    //Pauses the game
                    while(!playing) {
                        try {
                            sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    //Sleeps X seconds before counting down to achieve the right fuse speed
                    try {
                        sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //Makes the fuse count down every time the bomb arc reaches a checkpoint
                    if(arcStart - fuseCheckpoint == fuseInterval) {
                        //Prints the fuse value if debug mode is enabled
                        if(debugMode) {
                            System.out.println(fuse);
                        }

                        fuse--;
                        fuseCheckpoint += fuseInterval;
                    }

                    //Kills the loop when the game is over
                    if(isGameOver()) {
                        break;
                    }

                    gamePanel.repaint();
                }
            }
        };

        //Starts thread
        fuseThread.start();
    }

    //Shrinks the bomb when the player presses it to get clicks
    private void pressBomb() {
        if(playing & !isKeyDown & !isGameOver()) {
            textureLabel.setIcon(bomb_shrinked);
            isKeyDown = true;
            if(useMouse)
                startMousePressed();
        }
        else {
            if(playing & !isGameOver()) {
                textureLabel.setIcon(bomb);
            }
        }
    }

    //Releases the bomb, adds one click to the score and resets the bomb size
    private void releaseBomb() {
        //Checks if the game is playing
        if(playing & !isGameOver()) {
            textureLabel.setIcon(bomb);
            clicks++;
            isKeyDown = false;
        }
    }

    //Calculates the players current level record
    private int getLevelRecord(int currentLevel) {
        if(currentLevel > levelRecord) {
            this.levelRecord = currentLevel;
        }
        return levelRecord;
    }

    //Sets the fuse before math
    private int calculateNextFuse() {
        if(this.lastFuse > 5) {
            this.fuse = this.lastFuse - 1;
            this.lastFuse = this.fuse;
        }
        else {
            this.fuse = 5;
        }
        return this.fuse;
    }

    //Creates and shows the game window when called
    private void setupWindow() {

        //Game window
        window = new JFrame();
        window.setResizable(true);
        window.setSize(800, 700);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //The panel that renders/contains the game
        gamePanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                renderGame(g);
            }
        };
        gamePanel.setLayout(new BorderLayout());
        gamePanel.setBackground(gameBackground);

        //Declares textures
        bomb = new ImageIcon(new ImageIcon(getClass().getResource("/resources/Bomb.png")).getImage().getScaledInstance(bombWidth, bombHeight, Image.SCALE_DEFAULT));
        bomb_shrinked = new ImageIcon(new ImageIcon(getClass().getResource("/resources/Bomb.png")).getImage().getScaledInstance(shrinkedBombWidth, shrinkedBombHeight, Image.SCALE_DEFAULT));
        bomb_defused = new ImageIcon(new ImageIcon(getClass().getResource("/resources/Bomb_defused.png")).getImage().getScaledInstance(bombWidth, bombHeight, Image.SCALE_DEFAULT));
        explosion = new ImageIcon(new ImageIcon(getClass().getResource("/resources/Explosion.png")).getImage().getScaledInstance(bombWidth, bombHeight, Image.SCALE_DEFAULT));

        textureLabel = new JLabel(bomb, JLabel.CENTER);
        gamePanel.add(textureLabel);

        fpsCounter = new JLabel(fps + " Fps ", JLabel.RIGHT);
        gamePanel.add(fpsCounter, BorderLayout.NORTH);
        fpsCounter.setForeground(gameForeground);
        fpsCounter.setFont(gameFont);

        buttomText = new JPanel();
        gamePanel.add(buttomText, BorderLayout.SOUTH);
        buttomText.setLayout(new GridLayout(1,2));
        buttomText.setBackground(gameBackground);

        levelStats = new JPanel();
        buttomText.add(levelStats);
        levelStats.setBackground(gameBackground);
        levelStats.setLayout(new BorderLayout());

        levelCounter = new JLabel("Level " + currentLevel);
        levelStats.add(levelCounter, BorderLayout.WEST);
        levelCounter.setForeground(gameForeground);
        levelCounter.setFont(gameFont);

        recordLevel = new JLabel();
        levelStats.add(recordLevel);
        recordLevel.setForeground(gameForeground);
        recordLevel.setFont(gameFont);

        debugTag = new JLabel("Debug mode ", JLabel.RIGHT);
        buttomText.add(debugTag);
        debugTag.setForeground(gameForeground);
        debugTag.setFont(gameFont);
        debugTag.setVisible(debugMode);

        //Re-centers the time arc when the window is resized
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                arcX = debugTag.getX() - 147;
                arcY = (window.getHeight() - 300) / 2 - 20;
            }
        });

        /**
         * Keyboard controls
         */
        window.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {

                //Starts game!
                if(e.getKeyCode() == e.VK_ENTER) {
                    if(startScreen) {
                        startScreen = false;
                        startFuseThread(fuse);
                    }
                }

                //Presses bomb
                if(e.getKeyCode() == e.VK_SPACE) {
                    if(!useMouse) {
                        pressBomb();
                    }
                }

                //Pause function
                if(e.getKeyCode() == e.VK_ESCAPE) {
                    if(!startScreen & !isGameOver()) {
                        if(playing) {
                            playing = false;
                            System.out.println("Paused!");
                        }
                        else {
                            playing = true;
                            System.out.println("Resumed!");
                        }
                    }
                }

                //DebugMode cheats
                if(playing & debugMode) {
                    if(e.getKeyCode() == e.VK_W) {
                        clicks = currentFinishLine;
                    }
                    if(e.getKeyCode() == e.VK_L) {
                        fuse = 0;
                        arcStart = 360;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

                //Second part of the defuse bomb function
                if(e.getKeyCode() == e.VK_SPACE) {
                    if(!useMouse) {
                        releaseBomb();
                    }
                }

                //Enables mouse controls
                if(e.getKeyCode() == e.VK_M) {
                    if(useMouse) {
                        useMouse = false;
                        System.out.println("Disabled mouse controls!");
                    }
                    else {
                        useMouse = true;
                        System.out.println("Enabled mouse controls!");
                    }
                    gamePanel.repaint();
                }

                //Respawn function
                if(e.getKeyCode() == e.VK_R) {
                    if(!playing & fuse == 0) {
                        arcStart = 0;
                        clicks = 0;
                        currentLevel = 1;
                        currentFinishLine = 30;
                        lastFinishLine = currentFinishLine;
                        levelCounter.setText("Level " + currentLevel);
                        textureLabel.setIcon(bomb);
                        fuse = resetFuse;
                        lastFuse = resetFuse;
                        startFuseThread(fuse);
                    }
                }

                //Level up function
                if(e.getKeyCode() == e.VK_ENTER) {
                    if(!playing & clicks == currentFinishLine) {
                        currentLevel++;
                        clicks = 0;
                        arcStart = 0;
                        currentFinishLine = lastFinishLine + 10;
                        lastFinishLine = currentFinishLine;
                        textureLabel.setIcon(bomb);
                        levelCounter.setText("Level " + currentLevel);
                        startFuseThread(calculateNextFuse());
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {}
        });


        /**
         * Mouse controls
         */
        window.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(useMouse) {
                    //First part of the defuse bomb function with mouse controls
                    if(e.getButton() == MouseEvent.BUTTON1) {
                        pressBomb();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(useMouse) {
                    //Second part of the defuse bomb function with mouse controls
                    if(e.getButton() == MouseEvent.BUTTON1) {
                        releaseBomb();
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}

            @Override
            public void mouseClicked(MouseEvent e) {}
        });

        //Adds the gamePanel and makes the window visible after initialization
        window.add(gamePanel);
        window.setVisible(true);
        runGameLoop = true;
    }

    /**
     * Renders the game
     * @param g the graphics object
     */
    private void renderGame(Graphics g) {
        //Sets the window title and keeps it updated
        window.setTitle("Defuse! | " + fps + " Fps");

        //Styles game
        g.setFont(gameFont);
        g.setColor(gameForeground);

        //Game-information
        g.drawString("Press M to enable the mouse!", 5, 35);
        g.drawString("Press ESC to pause the game!", 5, 55);

        //Draws the use mouse info on screen
        if(useMouse) {
            g.drawString("Press LEFT-MOUSE to get clicks!", 5, 15);
            g.drawString("Mouse enabled!", arcX + 85, arcY + 365);
        }
        else {
            g.drawString("Press SPACE to get clicks!", 5, 15);
        }

        //Draws the fuse arc
        g.drawArc(arcX, arcY, 300, 300, 0, arcStart);

        //Game status
        if(!playing & startScreen) {
            g.drawString("Press ENTER to start the game!", arcX + 10 + splashX, arcY + 325);
        }
        else if(!playing & !startScreen & !isGameOver()) {
            g.drawString("Game Paused!", arcX + 90, arcY + 325);
        }
        else if (playing & !startScreen) {
            g.drawString("Time left: " + this.fuse, arcX + 90, arcY + 325);
        }

        //Identifies a win or los
        if(!playing & this.fuse == 0) {
            g.drawString("Bomb detonated! Press R to retry.", arcX - 10 + splashX, arcY + 325);
        }
        else if(!playing & clicks == currentFinishLine) {
            g.drawString("Bomb defused! Press ENTER to start next level.", arcX - 70 + splashX, arcY + 325);
        }

        //Clicks counter
        g.drawString("Clicks: " + clicks + "/" + currentFinishLine, arcX + 90, arcY + 345);
    }

    /**
     * Main method
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        switch(args.length) {
            case 1:
                new Game(args[0], 10);
                break;
            case 2:
                new Game(args[0], Integer.valueOf(args[1]));
                break;
            default:
                new Game("noDebugMode", 10);
        }
    }
}