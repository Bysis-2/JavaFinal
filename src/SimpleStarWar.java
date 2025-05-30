import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sound.sampled.*;
import java.io.File;
/**
 * Main game panel for Simple Star War.
 * Handles game loop, input, rendering, and sound.
 */
public class SimpleStarWar extends JPanel implements ActionListener, KeyListener {
     /** Background music clip. */
    private Clip backgroundMusic;

    /** The player’s ship. */
    private PlayerShip ship;

    /** List of straight-firing player bullets. */
    private List<Point> bullets = new ArrayList<>();

    /** List of enemy bullets. */
    private List<Point> enemyBullets = new ArrayList<>();

    /** List of enemy ships. */
    private List<Enemy> enemies = new ArrayList<>();

    /** Timer accumulator for spawning enemies (ms). */
    private int spawnTimer = 0;

    /** Current player score. */
    private int score = 0;

     /** Player health points. */
    private int playerHP = 3;

    /** Flag indicating game over state. */
    private boolean gameOver = false;

    /** Flag indicating player victory state. */
    private boolean gameWin = false;

    /** Swing timer driving the game loop (≈16 ms tick). */
    private Timer timer;

    /** Timer for continuous mouse shooting. */
    private Timer mouseShootTimer;

    /** Timer for continuous spacebar shooting. */
    private Timer spaceShootTimer;

    /** Highest score recorded this session. */
    private int highScore = 0;

    /** List of diagonal bullets (±60°) fired by player. */
    private List<DiagonalBullet> diagonalBullets = new ArrayList<>();

    /** Flag to show "level up" message. */
    private boolean showLevelUp = false;

    /** Timer accumulator for level-up message duration. */
    private int levelUpTimer = 0;

    /** Last level at which the "level up" was shown. */
    private int lastLevelShown = 0;

    /** Number of bullets each enemy fires per shoot event. */
    private int enemyFireCountPerShot = 1;

    /** Flag to show the main menu. */
    private boolean showMenu = true;

    /** Rectangle for the "Start Game" button area. */
    private Rectangle startButton = new Rectangle(180, 220, 120, 40);

    /** Rectangle for the "Exit Game" button on the menu. */
    private Rectangle exitButton = new Rectangle(180, 280, 120, 40);

    /** Rectangle for the "Restart" button on game over. */
    private Rectangle restartButton = new Rectangle(150, 200, 180, 40);

    /** Rectangle for the "Back to Menu" button on game over. */
    private Rectangle backToMenuButton = new Rectangle(150, 260, 180, 40);

    /** Rectangle for the "Exit Game" button on game over. */
    private Rectangle exitButtonGameOver = new Rectangle(150, 320, 180, 40);

    /** Cooldown interval between enemy spawns (ms). */
    private int spawnCooldown = 250;

    /**
     * Constructs the game panel, initializes input listeners, timers, and starts the loop.
     */
    public SimpleStarWar() {
        setPreferredSize(new Dimension(480, 500));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        ship = new PlayerShip(200, 450);

        // Mouse movement and drag for ship
        MouseMotionAdapter mouseMotion = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!gameOver) {
                    ship.setX(e.getX());
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!gameOver) {
                    ship.setX(e.getX());
                    repaint();
                }
            }
        };
        addMouseMotionListener(mouseMotion);

        // Mouse press and release for continuous shooting
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (showMenu) {
                    if (startButton.contains(e.getPoint())) {
                        showMenu = false;
                        resetGame();
                        playBackgroundMusic();
                        repaint();
                        return;
                    }
                    if (exitButton.contains(e.getPoint())) {
                        System.exit(0);
                    }
                    return;
                }
                
                if (gameOver) {
                    if (restartButton.contains(e.getPoint())) {
                        resetGame();
                        playBackgroundMusic();
                    } else if (backToMenuButton.contains(e.getPoint())) {
                        resetGame();
                        showMenu = true;
                        repaint();
                    } else if (exitButtonGameOver.contains(e.getPoint())) {
                        System.exit(0);
                    }
                    return;
                }
                if (!gameOver && e.getButton() == MouseEvent.BUTTON1) {
                    bullets.add(new Point(ship.getX() + PlayerShip.WIDTH / 2 - 2, ship.getY()));
                    playSoundEffect("player_shoot.wav");
                    if (score >= 100) {
                        int x = ship.getX() + PlayerShip.WIDTH / 2 - 2;
                        int y = ship.getY();
                        diagonalBullets.add(new DiagonalBullet(x, y, 60));
                        diagonalBullets.add(new DiagonalBullet(x, y, 120));
                    }


                    mouseShootTimer = new Timer(200, evt -> {//deal with dragged mouse shooting
                        bullets.add(new Point(ship.getX() + PlayerShip.WIDTH / 2 - 2, ship.getY()));
                        
                        if (score >= 100) {
                            int x1 = ship.getX() + PlayerShip.WIDTH / 2 - 2;
                            int y1 = ship.getY();
                            diagonalBullets.add(new DiagonalBullet(x1, y1, 60));
                            diagonalBullets.add(new DiagonalBullet(x1, y1, 120));
                        }

                        playSoundEffect("player_shoot.wav");
                    });
                    mouseShootTimer.start();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {//release than stop counting
                if (e.getButton() == MouseEvent.BUTTON1 && mouseShootTimer != null) {
                    mouseShootTimer.stop();
                }
            }
        });

        timer = new Timer(16, this);
        timer.start();
    }
    /**
     * Plays background music in a continuous loop.
     */
    private void playBackgroundMusic() {
        try {
            File musicFile = new File("bgm.wav");
            System.out.println("Loading bgm.wav from: " + musicFile.getAbsolutePath());
            if (!musicFile.exists()) {
                System.out.println("Error: bgm.wav not found");
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            System.out.println("Music started successfully");
        } catch (Exception e) {
            System.out.println("Error playing music: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Stops and closes the background music clip if playing.
     */
    private void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
            backgroundMusic.close();
        }
    }
    /**
     * Plays a one-shot sound effect from the given file path.
     *
     * @param filePath relative path to the sound file
     */
    private void playSoundEffect(String filePath) {
        try {
            File soundFile = new File(filePath);
            if (!soundFile.exists()) {
                System.out.println("Error: Sound file not found - " + filePath);
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            System.out.println("Error playing sound effect: " + e.getMessage());
        }
    }
    /**
     * Handles all rendering: menu, game over screens, level-up message,
     * player ship, bullets, enemies, and HUD.
     *
     * @param g Graphics context for drawing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (showMenu) {
            stopBackgroundMusic();
            String title = "星際大戰";
            Font titleFont = new Font("Microsoft JhengHei", Font.BOLD, 36);
            if (!titleFont.canDisplay('星')) {
                titleFont = new Font("Noto Sans TC", Font.BOLD, 36);
            }
            g.setFont(titleFont);
            FontMetrics fmTitle = g.getFontMetrics(titleFont);
            int titleWidth = fmTitle.stringWidth(title);
            int titleX = (getWidth() - titleWidth) / 2;
            g.setColor(Color.WHITE);
            g.drawString(title, titleX, 150);
        
            g.setColor(Color.GRAY);
            g.fillRect(startButton.x, startButton.y, startButton.width, startButton.height);
        
            String btnText = "開始遊戲";
            Font btnFont = new Font("Microsoft JhengHei", Font.BOLD, 20);
            if (!btnFont.canDisplay('開')) {
                btnFont = new Font("Noto Sans TC", Font.BOLD, 20);
            }
            g.setFont(btnFont);
            FontMetrics fmBtn = g.getFontMetrics(btnFont);
            int textWidth = fmBtn.stringWidth(btnText);
            int textHeight = fmBtn.getAscent();
            int textX = startButton.x + (startButton.width - textWidth) / 2;
            int textY = startButton.y + (startButton.height + textHeight) / 2 - 4;
        
            g.setColor(Color.BLACK);
            g.drawString(btnText, textX, textY);

            g.setColor(Color.GRAY);
            g.fillRect(exitButton.x, exitButton.y, exitButton.width, exitButton.height);

            String exitText = "離開遊戲";
            Font exitFont = new Font("Microsoft JhengHei", Font.BOLD, 20);
            if (!exitFont.canDisplay('離')) {
                exitFont = new Font("Noto Sans TC", Font.BOLD, 20);
            }
            g.setFont(exitFont);
            FontMetrics fmExit = g.getFontMetrics(exitFont);
            int exitTextWidth = fmExit.stringWidth(exitText);
            int exitTextHeight = fmExit.getAscent();
            int exitTextX = exitButton.x + (exitButton.width - exitTextWidth) / 2;
            int exitTextY = exitButton.y + (exitButton.height + exitTextHeight) / 2 - 4;

            g.setColor(Color.BLACK);
            g.drawString(exitText, exitTextX, exitTextY);

            g.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 16));
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("最高分：" + highScore, 10, getHeight() - 10);

            return;
        }
        
        if (gameOver) {
            stopBackgroundMusic();
            g.setColor(Color.WHITE);

            Font titleFont = new Font("Microsoft JhengHei", Font.BOLD, 30);
            if (!titleFont.canDisplay('遊')) {
                titleFont = new Font("Noto Sans TC", Font.BOLD, 30);
            }
            g.setFont(titleFont);

            String message = gameWin ? "你贏了！" : "遊戲結束";
            int messageWidth = g.getFontMetrics().stringWidth(message);
            g.drawString(message, (getWidth() - messageWidth) / 2, 140);

            Font buttonFont = new Font("Microsoft JhengHei", Font.BOLD, 20);
            if (!buttonFont.canDisplay('再')) {
                buttonFont = new Font("Noto Sans TC", Font.BOLD, 20);
            }
            g.setFont(buttonFont);
            g.setColor(Color.GRAY);

            g.fillRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height);
            g.fillRect(backToMenuButton.x, backToMenuButton.y, backToMenuButton.width, backToMenuButton.height);
            g.fillRect(exitButtonGameOver.x, exitButtonGameOver.y, exitButtonGameOver.width, exitButtonGameOver.height);

            g.setColor(Color.BLACK);
            g.drawString("再來一場", restartButton.x + 50, restartButton.y + 25);
            g.drawString("返回主畫面", backToMenuButton.x + 35, backToMenuButton.y + 25);
            g.drawString("離開遊戲", exitButtonGameOver.x + 50, exitButtonGameOver.y + 25);

            g.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 16));
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("得分：" + score + " 最高分：" + highScore, 140, 380);

            return;
        }

        
        if (showLevelUp) {
            g.setColor(Color.ORANGE);
            Font msgFont = new Font("Microsoft JhengHei", Font.BOLD, 28);
            if (!msgFont.canDisplay('難')) {
                msgFont = new Font("Noto Sans TC", Font.BOLD, 28);
            }
            g.setFont(msgFont);
            String msg = "難度提升！";
            int msgWidth = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (getWidth() - msgWidth) / 2, getHeight() / 2);
        }
        
        ship.draw(g);

        g.setColor(Color.YELLOW);
        for (Point b : bullets) {
            g.fillRect(b.x, b.y, 4, 10);
        }

        g.setColor(Color.RED);
        for (Point b : enemyBullets) {
            g.fillRect(b.x, b.y, 4, 10);
        }
        g.setColor(Color.CYAN);
        for (DiagonalBullet db : diagonalBullets) {
            db.draw(g);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Score: " + score, 10, 20);
        g.drawString("HP: " + playerHP, 10, 40);

        for (Enemy e : enemies) {
            e.draw(g);
        }
    }
    
    /**
     * Game loop tick: spawn enemies, move and fire bullets,
     * handle collisions, update state, and repaint.
     *
     * @param e ActionEvent from Timer
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;
        if (showMenu || gameOver) return;
        int level = score / 50;
        spawnCooldown = Math.max(70, 250 - level * 30);//according to score born enemy faster

        if (level > lastLevelShown) {//every 50 points, enemy shoot one more bullet and player heal 2hp 
            showLevelUp = true;
            levelUpTimer = 0;
            lastLevelShown = level;
            enemyFireCountPerShot = level + 1;
            playerHP = Math.min(playerHP + 2, 5);
        }

        spawnTimer += 16;
        if (spawnTimer >= spawnCooldown) {//born enemy
            Enemy newEnemy = new Enemy((int)(Math.random() * 440));
            enemies.add(newEnemy);

            // enemy shoot when born
            enemyBullets.add(new Point(
                newEnemy.getBounds().x + newEnemy.getBounds().width / 2 - 2,
                newEnemy.getBounds().y + newEnemy.getBounds().height
            ));
            playSoundEffect("enemy_shoot.wav");

            spawnTimer = 0;
        }

        for (Enemy enemy : enemies) {//enemy shoot
            if (enemy.shouldStartShooting()) {
                enemy.scheduleFire(enemyFireCountPerShot);
            }
            if (enemy.updateFire()) {
                enemyBullets.add(new Point(
                    enemy.getBounds().x + enemy.getBounds().width / 2 - 2,
                    enemy.getBounds().y + enemy.getBounds().height
                ));
                playSoundEffect("enemy_shoot.wav");
            }
        }

        Iterator<Point> bit = bullets.iterator();//moving player bullet and remove
        while (bit.hasNext()) {
            Point b = bit.next();
            b.y -= 8;
            if (b.y < 0) bit.remove();
        }


        Iterator<Point> ebit = enemyBullets.iterator();//moving enemy bullet and remove
        while (ebit.hasNext()) {
            Point b = ebit.next();
            b.y += 8;
            if (b.y > getHeight()) ebit.remove();
        }

        for (Enemy enemy : enemies) {//update enemy
            enemy.update();
        }

        enemies.removeIf(enemy -> enemy.getY() > getHeight());

        if (enemies.size() > 25) {//more than 25 ememies than lose
            gameOver = true;
            timer.stop();
            if (mouseShootTimer != null) mouseShootTimer.stop();
            if (spaceShootTimer != null) spaceShootTimer.stop();
            repaint();
            if (score > highScore) {
                highScore = score;
            }
            return;
        }

        Iterator<Enemy> eit = enemies.iterator();
        while (eit.hasNext()) {
            Enemy enemy = eit.next();
            boolean removed = false;

            // determine bullet collide
            Iterator<Point> bit2 = bullets.iterator();
            while (bit2.hasNext()) {
                Point b = bit2.next();
                Rectangle shot = new Rectangle(b.x, b.y, 4, 10);
                if (shot.intersects(enemy.getBounds())) {
                    eit.remove();
                    bit2.remove();
                    score++;
                    removed = true;
                    break;
                }
            }
            if (removed) continue;

            // determine DiagonalBullet collide
            Iterator<DiagonalBullet> dbit2 = diagonalBullets.iterator();
            while (dbit2.hasNext()) {
                DiagonalBullet db = dbit2.next();
                if (db.getBounds().intersects(enemy.getBounds())) {
                    eit.remove();
                    dbit2.remove();
                    score++;
                    break;
                }
            }
        }


        // ★ update DiagonalBullet
        Iterator<DiagonalBullet> dbit = diagonalBullets.iterator();
        while (dbit.hasNext()) {
            DiagonalBullet db = dbit.next();
            db.move();
            if (db.isOutOfBounds(getWidth(), getHeight())) {
                dbit.remove();
            }
        }

        Iterator<Point> ebit2 = enemyBullets.iterator();//player hit by enemy bullet
        while (ebit2.hasNext()) {
            Point b = ebit2.next();
            Rectangle shot = new Rectangle(b.x, b.y, 4, 10);
            if (shot.intersects(ship.getBounds())) {
                ebit2.remove();
                playerHP -= 1;
                if (playerHP <= 0) {
                    playerHP = 0;
                    gameOver = true;
                    if (score > highScore) {
                        highScore = score;
                    }                
                    timer.stop();
                    if (mouseShootTimer != null) mouseShootTimer.stop();
                    if (spaceShootTimer != null) spaceShootTimer.stop();
                }
            }
        }
        if (showLevelUp) {//message appear for 1 second
            levelUpTimer += 16;
            if (levelUpTimer >= 1000) {
                showLevelUp = false;
            }
        }
        if (!gameWin && score >= 450) {//win condition
            gameWin = true;
            gameOver = true;
            timer.stop();
            if (mouseShootTimer != null) mouseShootTimer.stop();
            if (spaceShootTimer != null) spaceShootTimer.stop();
            if (score > highScore) highScore = score;
        }

        
        repaint();
    }
    
    /**
     * Handles keyboard presses for moving and shooting.
     *
     * @param e KeyEvent containing key code
     */
    @Override
    public void keyPressed(KeyEvent e) {//keyboard control
        if (gameOver) return;

        int code = e.getKeyCode();
        if (code == KeyEvent.VK_LEFT) {
            ship.moveLeft(getWidth());
        } else if (code == KeyEvent.VK_RIGHT) {
            ship.moveRight(getWidth());
        } else if (code == KeyEvent.VK_SPACE) {
            bullets.add(new Point(ship.getX() + PlayerShip.WIDTH / 2 - 2, ship.getY()));
            playSoundEffect("player_shoot.wav");
            if (spaceShootTimer == null || !spaceShootTimer.isRunning()) {
                spaceShootTimer = new Timer(500, evt -> {
                    bullets.add(new Point(ship.getX() + PlayerShip.WIDTH / 2 - 2, ship.getY()));
                    playSoundEffect("player_shoot.wav");
                });
                spaceShootTimer.start();
            }
        }
    }

    private void resetGame() {//reset game
        score = 0;
        playerHP = 3;
        gameOver = false;
        gameWin = false;
        bullets.clear();
        enemyBullets.clear();
        enemies.clear();
        diagonalBullets.clear(); 
        ship.setX(200);
        spawnTimer = 0;
        spawnCooldown = 250; 
        enemyFireCountPerShot = 1; 
        lastLevelShown = 0; 
        showLevelUp = false;

        if (mouseShootTimer != null) mouseShootTimer.stop();
        if (spaceShootTimer != null) spaceShootTimer.stop();
        timer.start();
        repaint();
    }

    /** {@inheritDoc} */
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && spaceShootTimer != null) {
            spaceShootTimer.stop();
        }
    }
     /** {@inheritDoc} */
    @Override
    public void keyTyped(KeyEvent e) { }
    /**
     * Resets all game state to start a new game.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simple Star War - with Enemy Attacks");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new SimpleStarWar());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}