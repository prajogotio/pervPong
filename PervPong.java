
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by prajogotio on 19/1/15.
 */
public class PervPong {
    public static ArrayList<Ball> balls;
    public static ArrayList<Block> blocks;
    public static PongPad pongPad;
    public static ActiveAgentList activeAgentList;
    public static PervPongCanvas canvas;
    public static CommandList commandList;
    public static JFrame frame;
    public static GameLoopTask gameLoopTask;
    public static int level;
    public static BufferedImage background;
    public static Timer gameLoop;
    public static JPanel userPanel;
    public static JLayeredPane basePanel;
    public static JButton promptButton;
    public static boolean godLike;
    public static long godLikeBeginTime;
    public static JLabel levelLabel;
    public static JLabel scoreLabel;
    public static int playerScore;

    public static void main(String[] args) {
        initializeState();
        initializeAgents();
        initializeFrame();
        initalizeGameLoop();
    }

    public static void initializeState() {
        balls = new ArrayList<Ball>();
        blocks = new ArrayList<Block>();
        activeAgentList = new ActiveAgentList();
        level = 1;
        playerScore = 0;
        godLike = false;
        loadLevelImage();
        canvas = new PervPongCanvas(activeAgentList, background);
        commandList = new CommandList();
    }

    public static void initializeAgents() {
        pongPad = new PongPad(Configuration.SCREEN_WIDTH/2, Configuration.SCREEN_HEIGHT-PongPad.PONGPAD_HEIGHT, commandList);
        activeAgentList.add(pongPad);
        initializeBlocks();
        initializeBall();
    }

    public static void initializeBall() {
        balls.add(new Ball(Configuration.SCREEN_WIDTH/2, Configuration.SCREEN_HEIGHT - PongPad.PONGPAD_HEIGHT - Ball.BALL_RADIUS - 4, Math.random()*120 + 210));
        activeAgentList.add(balls.get(0));
    }

    public static void initializeBlocks() {
        for (int i = 6; i < Configuration.SCREEN_HEIGHT / (2 * Block.BLOCK_HEIGHT); ++i) {
            for (int j = 1; j < Configuration.SCREEN_WIDTH / Block.BLOCK_WIDTH - 1; ++j) {
                Block currentBlock = BlockGenerator.generate(j * Block.BLOCK_WIDTH, i * Block.BLOCK_HEIGHT + 90);
                blocks.add(currentBlock);
                activeAgentList.add(currentBlock);
            }
        }
    }

    public static void initializeFrame() {
        frame = new JFrame();
        basePanel = new JLayeredPane();
        basePanel.add(canvas, new Integer(0), 0);
        canvas.setBounds(0, 0, (int)Configuration.SCREEN_WIDTH, (int) Configuration.SCREEN_HEIGHT);
        basePanel.setPreferredSize(new Dimension((int)Configuration.SCREEN_WIDTH, (int)Configuration.SCREEN_HEIGHT));
        basePanel.setBounds(0, 0, (int) Configuration.SCREEN_WIDTH, (int) Configuration.SCREEN_HEIGHT);
        frame.setTitle("pervPong");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(basePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        userPanel = new JPanel(null);
        userPanel.setOpaque(false);

        promptButton = new JButton("Play Again!");
        promptButton.setBounds((int)Configuration.SCREEN_WIDTH/2 - 125, (int)Configuration.SCREEN_HEIGHT/2 - 50, 250, 100);
        promptButton.setFont(new Font("Helvetica", Font.BOLD, 20));

        userPanel.setBounds(0, 0, (int) Configuration.SCREEN_WIDTH, (int) Configuration.SCREEN_HEIGHT);
        basePanel.add(userPanel, new Integer(1), 1);

        levelLabel = new JLabel();
        levelLabel.setFont(new Font("Tahoma", Font.ITALIC, 40));
        levelLabel.setForeground(new Color(255, 255, 255));
        levelLabel.setText("Level " + level);
        levelLabel.setHorizontalAlignment(SwingConstants.CENTER);
        levelLabel.setBounds((int) Configuration.SCREEN_WIDTH/2 - 100, 0, 200, 70);
        userPanel.add(levelLabel);

        scoreLabel = new JLabel();
        scoreLabel.setFont(new Font("Times New Roman", Font.BOLD, 28));
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setText("0");
        scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        scoreLabel.setBounds((int) Configuration.SCREEN_WIDTH - 90, 40, 70, 30);
        userPanel.add(scoreLabel);

        promptButton.setText("Space/Enter to Start");
        userPanel.add(promptButton);
        promptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userPanel.remove(promptButton);
                restartGame();
            }
        });

        GameKeyListener gameKeyListener = new GameKeyListener(commandList);
        frame.addKeyListener(gameKeyListener);
        promptButton.requestFocus();
    }

    public static void initalizeGameLoop() {
        gameLoopTask = new GameLoopTask(activeAgentList, canvas);

        gameLoop = new Timer(Configuration.DISPLAY_RATE, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                collisionCheck();
                updateScoreLabel();
                gameLoopTask.run();
                gameStateCheck();
            }

        });

    }

    public static void collisionCheck() {
        double pongPadLeft = pongPad.getPosition().getLeft();
        double pongPadTop = pongPad.getPosition().getTop();
        for (int i = 0; i < balls.size(); ++i) {
            Ball curBall = balls.get(i);
            double ballLeft = curBall.getPosition().getLeft();
            double ballTop = curBall.getPosition().getTop();
            double radius = curBall.getRadius();
            for (int j = 0; j < blocks.size(); ++j) {
                Block curBlock = blocks.get(j);
                if(!curBlock.isAlive())continue;
                double blockLeft = curBlock.getPosition().getLeft();
                double blockTop = curBlock.getPosition().getTop();
                if(Math.abs(ballLeft - blockLeft) < (Block.BLOCK_WIDTH  + radius)/2
                        && Math.abs(ballTop - blockTop) < (Block.BLOCK_HEIGHT + radius)/2) {
                    playerScore += 500;
                    if(godLike) {
                        curBlock.setDead();
                        continue;
                    }
                    curBlock.handleHitByBall();
                    if(blockTop - Block.BLOCK_HEIGHT/2 + 4 <= ballTop &&
                            blockTop + Block.BLOCK_HEIGHT/2 - 4 >= ballTop){
                        curBall.handleHorizontalCollison();
                        if(ballLeft < blockLeft) curBall.getPosition().setLeft(blockLeft - Block.BLOCK_WIDTH/2 - radius/2 - 4);
                        else curBall.getPosition().setLeft(blockLeft + Block.BLOCK_WIDTH/2 + radius/2 + 4);
                    } else {
                        curBall.handleVerticalCollision();
                        if(ballTop < blockTop) curBall.getPosition().setTop(blockTop - Block.BLOCK_HEIGHT/2 - radius/2 - 4);
                        else curBall.getPosition().setTop(blockTop + Block.BLOCK_HEIGHT/2 + radius/2 + 4);
                    }
                    break;
                }
            }
            if (Math.abs(pongPadLeft - ballLeft) < (PongPad.PONGPAD_WIDTH + radius)/2
                    && Math.abs(pongPadTop - ballTop) < (PongPad.PONGPAD_HEIGHT + radius)/2){
                if (pongPadTop - PongPad.PONGPAD_HEIGHT/2 + 4 <= ballTop &&
                        pongPadTop + PongPad.PONGPAD_HEIGHT/2 - 4 >= ballTop) {
                    curBall.handleHorizontalCollison();
                } else {
                    //curBall.handleVerticalCollision();
                    double fraction = (ballLeft - pongPadLeft)/PongPad.PONGPAD_WIDTH;
                    curBall.setAngle(fraction * 120 + 270);
                }
            }
        }

        ArrayList<Block> tempBlock = new ArrayList<Block>();
        for (int i = 0; i < blocks.size(); ++i) {
            if(blocks.get(i).isAlive()) tempBlock.add(blocks.get(i));
        }
        ArrayList<Ball> tempBall = new ArrayList<Ball>();
        for (int i = 0; i < balls.size(); ++i) {
            if(balls.get(i).isAlive()) tempBall.add(balls.get(i));
        }
        balls = tempBall;
        blocks = tempBlock;
        activeAgentList.updateActiveAgentList();
    }

    public static void gameStateCheck() {
        if (balls.isEmpty()) {
            gameOver();
            return;
        }
        if (blocks.isEmpty()) {
            levelCompletedHandler();
            return;
        }
        if (godLike) {
            if (System.currentTimeMillis() - godLikeBeginTime >= 5000) godLike = false;
        }
    }

    public static void levelCompletedHandler() {
        gameLoop.stop();
        promptButton.setText("Level Completed!");
        level++;
        userPanel.add(promptButton);
        promptButton.requestFocus();
    }

    public static void gameOver() {
        gameLoop.stop();
        promptButton.setText("Game Over");
        userPanel.add(promptButton);
        playerScore = 0;
        promptButton.requestFocus();
    }

    public static void loadLevelImage() {
        try {
            //background = ImageIO.read(new File(PervPong.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "/img/" + level + ".jpg"));
            background = ImageIO.read(PervPong.class.getResourceAsStream("/img/" + level + ".jpg"));
        } catch (Exception e) {
        }
    }

    public static void startAtLevel(int toLevel){
        level = toLevel;
        loadLevelImage();
        godLike = false;
        pongPad.getPosition().setLeft(Configuration.SCREEN_WIDTH/2);
        commandList.clearCommands();
        canvas.setBackground(background);
        activeAgentList.clearActiveAgents();
        balls.clear();
        blocks.clear();
        activeAgentList.add(pongPad);
        initializeBall();
        initializeBlocks();
    }

    public static void restartGame() {
        startAtLevel(level);
        levelLabel.setText("Level " + level);
        gameLoop.start();
        frame.requestFocus();
    }

    public static void updateScoreLabel() {
        scoreLabel.setText(new Integer(playerScore).toString());
    }

}

class Configuration {
    public static final double SCREEN_WIDTH = 500;
    public static final double SCREEN_HEIGHT = 700;
    public static final double EPS = 1e-20;
    public static final int DISPLAY_RATE = 1000/60;
}

class Position {
    private double left;
    private double top;

    Position(double left, double top){
        this.left = left;
        this.top = top;
    }

    public double getLeft() { return left; }
    public double getTop() { return top; }
    public void setLeft(double left) { this.left = left; }
    public void setTop(double top) { this.top = top; }
}

interface Renderable {
    public void renderSelf(Graphics2D g2d);
}

interface ActiveAgent {
    public void update();
    public boolean isAlive();
}

class Ball implements Renderable, ActiveAgent {
    public final static double BALL_RADIUS = 14;
    public final static Color BALL_COLOR = Color.RED;
    public final static double BALL_SPEED = 10;

    private Position position;
    private double radius;
    private double speed;
    private Color color;
    private double angle;
    private boolean isAlive;

    Ball (double left, double top, double angle) {
        position = new Position(left, top);
        radius = Ball.BALL_RADIUS;
        color = Ball.BALL_COLOR;
        speed = Ball.BALL_SPEED;
        this.angle = angle;
        isAlive = true;
    }

    @Override
    public void renderSelf(Graphics2D g2d) {
        AffineTransform savedContext = g2d.getTransform();
        AffineTransform currentContext = new AffineTransform();
        currentContext.translate(position.getLeft(), position.getTop());
        g2d.setTransform(currentContext);
        g2d.setColor(color);
        if(PervPong.godLike) g2d.setColor(new Color(255, 255, 0, 255));
        g2d.fillOval((int) -radius / 2, (int) -radius / 2, (int) radius, (int) radius);
        g2d.setTransform(savedContext);
    }

    @Override
    public void update() {
        updatePosition();
    }

    private void updatePosition() {
        double dx = Math.cos(angle / 180 * Math.PI) * speed;
        double dy = Math.sin(angle / 180 * Math.PI) * speed;
        double x = position.getLeft() + dx;
        double y = position.getTop() + dy;
        if (y < 0) {
            y = 0;
            handleVerticalCollision();
        }
        if (x < 0) {
            x = 0;
            handleHorizontalCollison();
        }
        if (y > Configuration.SCREEN_HEIGHT) {
            y = Configuration.SCREEN_HEIGHT;
            //handleVerticalCollision();
            isAlive = false;
        }
        if (x > Configuration.SCREEN_WIDTH) {
            x = Configuration.SCREEN_WIDTH;
            handleHorizontalCollison();
        }
        position.setLeft(x);
        position.setTop(y);
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    public void handleVerticalCollision() {
        double dx = Math.cos(angle / 180 * Math.PI) * speed;
        double dy = Math.sin(angle / 180 * Math.PI) * speed;
        angle = Math.atan2(-dy, dx) / Math.PI * 180;
    }

    public void handleHorizontalCollison() {
        double dx = Math.cos(angle / 180 * Math.PI) * speed;
        double dy = Math.sin(angle / 180 * Math.PI) * speed;
        angle = Math.atan2(dy, -dx) / Math.PI * 180;
    }

    public Position getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    public void setAngle(double alpha) {
        if(alpha < 0) alpha += 360;
        if (alpha < 20) alpha = 20;
        if (160 < alpha && alpha < 200) {
            if (alpha < 180) alpha = 160;
            if (alpha > 180) alpha = 200;
        }
        if (alpha > 340) alpha = 340;
        this.angle = alpha;
    }
}

class PervPongCanvas extends JPanel {
    ActiveAgentList activeAgentList;
    BufferedImage background;
    PervPongCanvas(ActiveAgentList activeAgentList, BufferedImage background) {
        setPreferredSize(new Dimension((int)Configuration.SCREEN_WIDTH, (int)Configuration.SCREEN_HEIGHT));
        this.activeAgentList = activeAgentList;
        this.background = background;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(background, 0, 0, (int)Configuration.SCREEN_WIDTH, (int)Configuration.SCREEN_HEIGHT, null);
        ActiveAgentIterator iterator = activeAgentList.getIterator();
        while(iterator.hasNext()) {
            Renderable renderable = (Renderable) iterator.getNext();
            renderable.renderSelf(g2d);
        }
    }

    public void setBackground(BufferedImage background) {
        this.background = background;
    }
}

class ActiveAgentList {
    private ArrayList<Object> activeAgents;

    ActiveAgentList() {
        activeAgents = new ArrayList<Object>();
    }

    public void add(ActiveAgent activeAgent) {
        activeAgents.add(activeAgent);
    }

    public ActiveAgentIterator getIterator() {
        return new ActiveAgentIterator(activeAgents);
    }

    public void updateActiveAgentList() {
        ArrayList<Object> temp = new ArrayList<Object>();
        for (int i = 0; i < activeAgents.size(); ++i) {
            ActiveAgent agent = (ActiveAgent) activeAgents.get(i);
            if (agent.isAlive()) {
                temp.add(agent);
            }
        }
        activeAgents = temp;
    }

    public void clearActiveAgents() {
        activeAgents.clear();
    }
}

class ActiveAgentIterator {
    private ArrayList<Object> activeAgents;

    private int currentPosition;
    ActiveAgentIterator(ArrayList<Object> activeAgents) {
        this.activeAgents = activeAgents;
        currentPosition = -1;
    }

    public boolean hasNext() {
        return currentPosition + 1 < activeAgents.size();
    }

    public Object getNext() {
        ++currentPosition;
        return activeAgents.get(currentPosition);
    }

}

class GameLoopTask {
    ActiveAgentList activeAgentList;
    PervPongCanvas gameCanvas;

    GameLoopTask (ActiveAgentList activeAgentList, PervPongCanvas gameCanvas) {
        this.activeAgentList = activeAgentList;
        this.gameCanvas = gameCanvas;
    }

    public void run() {
        ActiveAgentIterator iterator = activeAgentList.getIterator();
        while (iterator.hasNext()) {
            ActiveAgent agent = (ActiveAgent) iterator.getNext();
            agent.update();
        }

        gameCanvas.repaint();
    }
}

class GameKeyListener implements KeyListener {
    CommandList commandList;

    GameKeyListener(CommandList commandList) {
        this.commandList = commandList;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_UP) {

        }
        if(e.getKeyCode() == KeyEvent.VK_LEFT) {
            commandList.add("MOVE_LEFT");
        }
        if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
            commandList.add("MOVE_RIGHT");
        }
        if(e.getKeyCode() == KeyEvent.VK_DOWN) {

        }
        if(e.getKeyCode() == KeyEvent.VK_P) {

            PervPong.levelCompletedHandler();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {

        }
        if(e.getKeyCode() == KeyEvent.VK_LEFT) {
            commandList.remove("MOVE_LEFT");
        }
        if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
            commandList.remove("MOVE_RIGHT");
        }
        if(e.getKeyCode() == KeyEvent.VK_DOWN) {

        }
        if(e.getKeyCode() == KeyEvent.VK_N) {
        }
    }
}

class PongPad implements Renderable, ActiveAgent {
    public static final double PONGPAD_WIDTH = 120;
    public static final double PONGPAD_HEIGHT = 20;
    public static final double PONGPAD_SPEED = 12;
    public static final Color PONGPAD_COLOR = new Color(255,0,0,200);

    private Position position;
    private CommandList commandList;
    private double width;
    private double height;
    private double speed;
    private Color color;
    private boolean isAlive;
    private CommandList commands;

    PongPad(double left, double top, CommandList commandList) {
        position = new Position(left, top);
        width = PongPad.PONGPAD_WIDTH;
        height = PongPad.PONGPAD_HEIGHT;
        speed = PongPad.PONGPAD_SPEED;
        color = PongPad.PONGPAD_COLOR;
        this.commandList = commandList;
        isAlive = true;
    }

    @Override
    public void renderSelf(Graphics2D g2d) {
        AffineTransform savedContext = g2d.getTransform();
        AffineTransform currentContext = new AffineTransform();
        currentContext.translate(position.getLeft(), position.getTop());
        g2d.setTransform(currentContext);
        g2d.setColor(color);
        if(PervPong.godLike) g2d.setColor(new Color(255, 255, 0, 255));
        g2d.fillRect((int) -width / 2, (int) -height / 2, (int) width, (int) height);
        g2d.setTransform(savedContext);
    }

    @Override
    public void update() {
        if (commandList.hasCommand("MOVE_LEFT")) {
            double left = position.getLeft();
            left -= speed;
            if(left < 0) left = 0;
            position.setLeft(left);
        }
        if (commandList.hasCommand("MOVE_RIGHT")) {
            double left = position.getLeft();
            left += speed;
            if(left > Configuration.SCREEN_WIDTH) left = Configuration.SCREEN_WIDTH;
            position.setLeft(left);
        }
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    public Position getPosition() {
        return position;
    }
}

class CommandList {
    private Map<String, Boolean> commands;

    CommandList() {
        commands = new HashMap<String, Boolean>();
    }

    public void add(String command) {
        commands.put(command, true);
    }

    public void remove(String command) {
        if(commands.containsKey(command)) {
            commands.put(command, false);
        }
    }

    public boolean hasCommand(String command) {
        if(commands.containsKey(command)) {
            return commands.get(command);
        }
        return false;
    }

    public void clearCommands() {
        commands.clear();
    }
}


abstract class Block implements Renderable, ActiveAgent{
    public static double BLOCK_WIDTH = 80;
    public static double BLOCK_HEIGHT = 30;
    public static Color BLOCK_COLOR = new Color(0, 0, 0, 150);
    protected Position position;
    protected boolean isAlive;
    protected Color color;
    protected double width;
    protected double height;

    Block(double left, double top) {
        position = new Position(left, top);
        width = BLOCK_WIDTH;
        height = BLOCK_HEIGHT;
        color = BLOCK_COLOR;
        isAlive = true;
    }

    @Override
    public void update() {
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    @Override
    public void renderSelf(Graphics2D g2d) {
        AffineTransform savedContext = g2d.getTransform();
        AffineTransform currentContext = new AffineTransform();
        currentContext.translate(position.getLeft(), position.getTop());
        g2d.setTransform(currentContext);
        g2d.setColor(color);
        g2d.fillRect((int) -width / 2, (int) -height / 2, (int) width, (int) height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect((int)-width/2, (int)-height/2, (int)width, (int)height);
        g2d.setTransform(savedContext);
    }

    abstract public void handleHitByBall();

    public Position getPosition() {
        return position;
    }

    public void setDead() {
        isAlive = false;
    }
}


class NormalBlock extends Block {
    public static Color COLOR = new Color(0,0,0,200);
    public static int STRENGTH = 1;

    protected int healthPoint;

    NormalBlock(double left, double top) {
        super(left, top);
        healthPoint = STRENGTH;
        color = NormalBlock.COLOR;
    }

    @Override
    public void update() {
        if (healthPoint <= 0) isAlive = false;
    }

    public void handleHitByBall() {
        healthPoint--;
    }

}

class SuperBlock extends Block {
    public static int STRENGTH = 3;

    private ArrayList<Color> COLOR = new ArrayList<Color>();
    private int healthPoint;
    SuperBlock(double left, double top) {
        super(left, top);
        healthPoint = STRENGTH;
        COLOR.add(new Color(0,0,0,200));
        COLOR.add(new Color(100, 100, 0, 200));
        COLOR.add(new Color(150,70,0,200));
        color = COLOR.get(healthPoint-1);
    }

    @Override
    public void update() {
        if (healthPoint <= 0) isAlive = false;
    }

    public void handleHitByBall() {
        healthPoint--;
        if (healthPoint != 0) color = COLOR.get(healthPoint-1);
    }
}

class MightyBlock extends NormalBlock {

    MightyBlock (double left, double top) {
        super(left, top);
        healthPoint = 1;
        isAlive = true;
        color = new Color(220, 112, 134, 255);
    }

    @Override
    public void handleHitByBall() {
        healthPoint--;
    }

    @Override
    public void update() {
        if (healthPoint <= 0) {
            isAlive = false;
            Ball newBall = new Ball(position.getLeft(), position.getTop(), Math.random() * 360);
            PervPong.activeAgentList.add(newBall);
            PervPong.balls.add(newBall);
        }
    }
}

class GodBlock extends NormalBlock {

    GodBlock(double left, double top) {
        super(left, top);
        color = new Color(111, 11, 211, 202);
    }

    @Override
    public void update() {
        if(healthPoint <= 0) {
            isAlive = false;
            PervPong.godLike = true;
            PervPong.godLikeBeginTime = System.currentTimeMillis();
        }
    }
}

class BlockGenerator {
    public static Block generate(double left, double top) {
        if (Math.random() < 0.08) return new MightyBlock(left, top);
        else if(Math.random() < 0.04) return new GodBlock(left, top);
        else if(Math.random() > 0.2) return new NormalBlock(left, top);
        else return new SuperBlock(left, top);
    }
}
