import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;

import com.topcoder.marathon.*;

public class QueenAttackTester extends MarathonAnimatedVis {
  //parameter ranges
  private static final int minN = 8, maxN = 30;     // grid size range
  private static final int minC = 1, maxC = 6;      // number of colours range
  private static final double minP = 0, maxP = 0.25;      // wall probability range

  //Inputs
  private int N;            // grid size
  private int C;            // number of colours
  private double P;         // wall probability
  
  //Constants
  private static final int Empty=0;
  private static final int Wall=-1;
  
  //Graphics   
  Color[] colors={Color.cyan, Color.red, Color.green, Color.magenta, Color.orange, new Color(153,102,0)};
  private static Image Queen;

  //State Control
  private int[][] grid;                  // grid state
  private int[][] errors;                // errors at each cell
  private int numMoves;                  // number of moves used
  private int selectedR=-1;              // these are the selected locations for the current move
  private int selectedC=-1;
  private int selectedR2=-1;
  private int selectedC2=-1;
  private int errorR=-1;                 // these are the error locations for the current move
  private int errorC=-1;
  private int errorR2=-1;
  private int errorC2=-1;
  private int oldR=-1;                   //location of last piece
  private int oldC=-1;
  private ArrayList<Integer>[][] paths;  // paths taken by all the pieces
  private int last_move=-1;              // location of the last move
  private int current_path_length=0;     // length of the current path
  private int numErrors;                 // number of errors
  private double totalPenalty=0;         // total move penalty
  private double pathPenalty=0;          // total path penalty
  private double score;
  private int render_path_row=-1;
  private int render_path_col=-1;
  private boolean done=false;

  protected void generate()
  {
    N = randomInt(minN, maxN);
    C = randomInt(minC, maxC);
    P = randomDouble(minP, maxP);

    //Special cases
    if (seed == 1)
    {
      N = minN;
      C = 2;
    }
    else if (seed == 2)
    {
      N = maxN;
      C = maxC;
      P = maxP;
    }

    //User defined parameters
    if (parameters.isDefined("N")) N = randomInt(parameters.getIntRange("N"), minN, maxN);
    if (parameters.isDefined("C")) C = randomInt(parameters.getIntRange("C"), minC, maxC);
    if (parameters.isDefined("P")) P = randomDouble(parameters.getDoubleRange("P"), minP, maxP);

    errors = new int[N][N];

    //generate grid
    while(true)
    {
      grid = new int[N][N];
      for (int col=1; col<=C; col++)
      {
        for (int i=0; i<N; )
        {
          int r=randomInt(0, N-1);
          int c=randomInt(0, N-1);
          if (grid[r][c]==Empty)
          {
            grid[r][c]=col;
            i++;
          }
        }
      }
      for (int r=0; r<N; r++)
        for (int c=0; c<N; c++)
          if (grid[r][c]==Empty && randomDouble(0,1)<P)
            grid[r][c]=Wall;


      //check that there aren't too many walls in every row and column to make it unsolvable
      boolean ok=true;
      for (int i=0; i<N; i++)
      {
        int count1=0;
        int count2=0;
        for (int k=0; k<N; k++)
        {
          if (grid[i][k]==Wall) count1++;
          if (grid[k][i]==Wall) count2++;
        }
        if (count1>N-C || count2>N-C) ok=false;
      }
      if (ok) break;
    }

    numMoves=0;
    paths=new ArrayList[N][N];
    for (int r=0; r<N; r++)
    {
      for (int c=0; c<N; c++)
      {
        if (grid[r][c] > 0)
        {
          paths[r][c] = new ArrayList<Integer>();
          paths[r][c].add(r * N + c);
        }
      }
    }

    done = parameters.isDefined("manual");

    if (debug)
    {
      System.out.println("Grid size, N = " + N);
      System.out.println("Number of colours, C = " + C);
      System.out.println("Wall probability, P = "+P);
      System.out.println("Grid:");
      for (int r=0; r<N; r++)
      {
        for (int c=0; c<N; c++)
        {
          if (grid[r][c]==Empty) System.out.print(".");
          else if (grid[r][c]==Wall) System.out.print("#");
          else System.out.print(grid[r][c]);
        }
        System.out.println();
      }
    }
  }


  protected boolean isMaximize() {
      return false;
  }

  protected double run() throws Exception
  {
    init();

    if (parameters.isDefined("manual"))
    {
      setDefaultDelay(0);
      return 0;
    }
    else return runAuto();
  }


  protected double runAuto() throws Exception
  {
    double score = callSolution();
    done = true;
    if (score < 0) {
      if (!isReadActive()) return getErrorScore();
      return fatalError();
    }
    return score;
  }

  protected void timeout() {
    addInfo("Time", getRunTime());
    update();
  }

  protected void makeMove(int r1, int c1, int r2, int c2, double moveDist) {
    if (r1==oldR && c1==oldC)
    {
      current_path_length++;
      pathPenalty+=moveDist;
    }
    else
    {
      current_path_length = 1;
      pathPenalty=moveDist;
    }

    last_move = r2*N+c2;
    paths[r2][c2] = paths[r1][c1];
    paths[r1][c1] = null;
    paths[r2][c2].add(last_move);
    oldR=r2;
    oldC=c2;

    grid[r2][c2]=grid[r1][c1];
    grid[r1][c1]=Empty;
    selectedR=r2;
    selectedC=c2;
    selectedR2=r1;
    selectedC2=c1;
  }

  private double callSolution() throws Exception
  {
    writeLine(""+N);
    writeLine(""+C);
    for (int r=0; r<N; r++)
      for (int c=0; c<N; c++)
        writeLine(""+grid[r][c]);

    flush();
    if (!isReadActive()) return -1;


    updateState();

    try
    {
      //read solution output
      startTime();
      int n=Integer.parseInt(readLine());
      if (n<0 || n>2*N*N*N)
        return fatalError("Number of moves must between 0 and "+(2*N*N*N)+" inclusive");

      String[] moves=new String[n];
      for (int i=0; i<n; i++) moves[i]=readLine();
      stopTime();

      for (numMoves=1; numMoves<=n; numMoves++)
      {
        String move=moves[numMoves-1];
        String[] temp=move.split(" ");
        if (temp.length!=4)
          return fatalError("Each move must contain exactly 4 numbers separated by a space: " + moves[numMoves-1]);

        int r1=Integer.parseInt(temp[0]);
        int c1=Integer.parseInt(temp[1]);
        int r2=Integer.parseInt(temp[2]);
        int c2=Integer.parseInt(temp[3]);

        if (!inGrid(r1,c1))
          return fatalError("Location ("+r1+","+c1+") is not in the grid");
        if (!inGrid(r2,c2))
          return fatalError("Location ("+r2+","+c2+") is not in the grid");
        if (r1==r2 && c1==c2)
          return fatalError("Start and end locations cannot be the same: "+move);
        if (!(r1==r2 || c1==c2 || Math.abs(r1-r2)==Math.abs(c1-c2)))
          return fatalError("Not a legal queen move: "+move);
        if (grid[r1][c1]==Empty || grid[r1][c1]==Wall)
          return fatalError("Start location ("+r1+","+c1+") cannot be empty or wall");
        if (grid[r2][c2]!=Empty)
          return fatalError("End location ("+r2+","+c2+") must be empty");

        //check move
        int dr=0;
        if (r2>r1) dr=+1;
        if (r2<r1) dr=-1;

        int dc=0;
        if (c2>c1) dc=+1;
        if (c2<c1) dc=-1;

        for (int r=r1+dr, c=c1+dc; r!=r2 || c!=c2; r+=dr, c+=dc)
          if (grid[r][c]!=Empty)
            return fatalError("Cannot move through other pieces or walls "+move);

        //make move
        double moveDist=Math.sqrt(Math.max(Math.abs(r2-r1),Math.abs(c2-c1)));
        makeMove(r1, c1, r2, c2, moveDist);

        totalPenalty+=moveDist;
        countErrors();
        score=numErrors*N+totalPenalty;


        if (!parameters.isDefined("noanimate")) updateState();
      }
      if (parameters.isDefined("noanimate")) updateState();
    }
    catch (Exception e) {
      if (debug) System.out.println(e.toString());
      return fatalError("Cannot parse your output");
    }

    return score;
  }

  protected void countErrors()
  {
    numErrors=0;

    for (int r1=0; r1<N; r1++)
      for (int c1=0; c1<N; c1++)
      {
        errors[r1][c1]=0;
        if (grid[r1][c1]==Empty || grid[r1][c1]==Wall) continue;
        for (int r2=0; r2<N; r2++)
          for (int c2=0; c2<N; c2++)
          {
            if (r2==r1 && c2==c1) continue;
            if (grid[r1][c1]==grid[r2][c2] && (r1==r2 || c1==c2 || Math.abs(r1-r2)==Math.abs(c1-c2)))
            {
              numErrors++;
              errors[r1][c1]++;
            }
          }
      }

    numErrors/=2;
  }


  protected boolean inGrid(int r, int c)
  {
    return r>=0 && r<N && c>=0 && c<N;
  }


  protected void updateState()
  {
    if (hasVis())
    {
      synchronized (updateLock) {
        addInfo("Moves", numMoves);
        addInfo("Time",  getRunTime());
        addInfo("Errors", numErrors);
        addInfo("Total Penalty", shorten(totalPenalty));
        addInfo("Path Penalty", shorten(pathPenalty));
        addInfo("Score", shorten(score));
      }
      updateDelay();
    }
  }

  class SaveUpdate
  {
    public int r1,c1,r2,c2,l;
    double p;
    SaveUpdate(int R1, int C1, int R2, int C2, int L, double P)
    {
      r1 = R1;
      c1 = C1;
      r2 = R2;
      c2 = C2;
      l = L;
      p = P;
    }
  }
  Stack<SaveUpdate> updates = new Stack<SaveUpdate>();

  protected void undo()
  {
    if (updates.isEmpty()) return;

    SaveUpdate u = updates.pop();
    paths[u.r2][u.c2].remove(paths[u.r2][u.c2].size() - 1);
    paths[u.r1][u.c1] = paths[u.r2][u.c2];
    paths[u.r2][u.c2] = null;
    current_path_length--;
    double moveDist=Math.sqrt(Math.max(Math.abs(u.r2-u.r1),Math.abs(u.c2-u.c1)));
    totalPenalty-=moveDist;
    pathPenalty-=moveDist;

    if (current_path_length <= 0) {
      selectedR=-1;
      selectedC=-1;
      if (updates.isEmpty()) {
        last_move = -1;
        current_path_length = 0;
        pathPenalty = 0;
      } else {
        SaveUpdate l = updates.lastElement();
        last_move = l.r2 * N + l.c2;
        current_path_length = l.l;
        pathPenalty = l.p;
      }
    } else {
      last_move = u.r1 * N + u.c1;
      selectedR=u.r1;
      selectedC=u.c1;
    }

    grid[u.r1][u.c1]=grid[u.r2][u.c2];
    grid[u.r2][u.c2]=Empty;


    countErrors();
    score=numErrors*N+totalPenalty;

    numMoves--;
    selectedR2=-1;
    selectedC2=-1;
    oldR=selectedR;
    oldC=selectedC;
    updateState();
    updateDelay();
  }


  protected void contentClicked(double x, double y, int mouseButton, int clickCount)
  {
    if (!parameters.isDefined("manual")) return;

    //undo with right click
    if (mouseButton == java.awt.event.MouseEvent.BUTTON3)
    {
      undo();
      return;
    }

    int r = (int)Math.floor(y);
    int c = (int)Math.floor(x);

    if (!inGrid(r,c)) return;       //outside of grid

    //unselect piece
    if (r==selectedR && c==selectedC)
    {
      selectedR=-1;
      selectedC=-1;
      updateState();
      current_path_length = 0;
      pathPenalty = 0;
      return;
    }
    //select piece
    else if (grid[r][c] > 0)
    {
      selectedR=r;
      selectedC=c;
      selectedR2=-1;
      selectedC2=-1;      
      updateState();
      return;
    }
    if (selectedR == -1) return;

    if (grid[r][c]!=Empty) return;    //must select empty on second click

    if (!(r==selectedR || c==selectedC || Math.abs(r-selectedR)==Math.abs(c-selectedC))) return;   //not legal move

    //check move
    int dr=0;
    if (r>selectedR) dr=+1;
    if (r<selectedR) dr=-1;

    int dc=0;
    if (c>selectedC) dc=+1;
    if (c<selectedC) dc=-1;

    for (int r3=selectedR+dr, c3=selectedC+dc; r3!=r || c3!=c; r3+=dr, c3+=dc)
      if (grid[r3][c3]!=Empty)
        return;   //Cannot move through other pieces or walls


    selectedR2=r;
    selectedC2=c;

    //make move
    double moveDist=Math.sqrt(Math.max(Math.abs(selectedR2-selectedR),Math.abs(selectedC2-selectedC)));
    totalPenalty+=moveDist;
    makeMove(selectedR, selectedC, selectedR2, selectedC2, moveDist);
    updates.push(new SaveUpdate(selectedR2, selectedC2, selectedR, selectedC, current_path_length, pathPenalty));

    countErrors();
    score=numErrors*N+totalPenalty;

    numMoves++;
    updateState();    //show move and new score
  }

  protected void mouseMove(double x, double y) {
    if (!done) return;
    int r = (int)Math.floor(y);
    int c = (int)Math.floor(x);

    if (!inGrid(r,c)) return;       //outside of grid
    boolean update = false;
    if (grid[r][c] > 0) {
      if (render_path_row != r || render_path_col != c) {
        update = true;
      }
      render_path_row = r;
      render_path_col = c;
    } else {
      if (render_path_row >= 0) {
        update = true;
      }
      render_path_row = -1;
      render_path_col = -1;
    }
    if (update) updateDelay();
  }

  protected void drawPath(Graphics2D g, int r, int c) {
    int length = paths[r][c].size() - 1;
    if (r*N+c == last_move) {
      length -= current_path_length;
    }
    if (length > 0) {
      g.setColor(colors[grid[r][c]-1]);
      g.setStroke(new BasicStroke(0.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      GeneralPath gp = new GeneralPath();
      int r1=paths[r][c].get(0)/N;
      int c1=paths[r][c].get(0)%N;
      gp.moveTo(c1+0.5, r1+0.5);
      for (int i=1; i <= length; i++)
      {
        int r2=paths[r][c].get(i)/N;
        int c2=paths[r][c].get(i)%N;
        gp.lineTo(c2+0.5, r2+0.5);
      }
      g.draw(gp);
    }
    if (length + 1 < paths[r][c].size()) {
      g.setColor(Color.blue);
      g.setStroke(new BasicStroke(0.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      GeneralPath gp = new GeneralPath();
      int r1=paths[r][c].get(length)/N;
      int c1=paths[r][c].get(length)%N;
      gp.moveTo(c1+0.5, r1+0.5);
      for (int i=length + 1; i < paths[r][c].size(); i++)
      {
        int r2=paths[r][c].get(i)/N;
        int c2=paths[r][c].get(i)%N;
        gp.lineTo(c2+0.5, r2+0.5);
      }
      g.draw(gp);
    }
  }

  protected void drawPiece(Graphics2D g, int r, int c)
  {
    if (parameters.isDefined("noimages"))
    {
      g.setColor(colors[grid[r][c]-1]);
      Ellipse2D t = new Ellipse2D.Double(c + 0.15, r + 0.15, 0.7, 0.7);
      g.fill(t);

      //draw number of errors made by this piece
      g.setColor(Color.black);
      adjustFont(g, Font.SANS_SERIF, Font.PLAIN, ""+errors[r][c], new Rectangle2D.Double(0, 0, 0.67, 0.67));
      drawString(g, ""+errors[r][c], new Rectangle2D.Double(c+0.5, r+0.5, 0, 0));
    }
    else
    {
      g.drawImage(Queen,c,r,1,1,null);
    }
  }


  protected void paintContent(Graphics2D g)
  {
    g.setColor(Color.white);
    g.fillRect(0,0,N,N);

    //draw background
    for (int r=0; r<N; r++)
      for (int c=0; c<N; c++)
      {
        if (grid[r][c]==Wall)
        {
          g.setColor(Color.gray);
          g.fillRect(c,r,1,1);
        }
        else if (grid[r][c]!=Empty && !parameters.isDefined("noimages"))
        {
          g.setColor(colors[grid[r][c]-1]);
          g.fillRect(c,r,1,1);
        }
      }

    //draw all paths
    if (parameters.isDefined("showAllPaths"))
    {
      for (int r=0; r<N; r++)
        for (int c=0; c<N; c++)      
          if (paths[r][c]!=null)
            drawPath(g, r, c);
    }

    //draw path for mouse hover
    if (render_path_row != -1) {
      drawPath(g, render_path_row, render_path_col);
    }

    //draw path of current queen
    if (selectedR!=-1)
    {
      drawPath(g, selectedR, selectedC);
      //draw current colour on top of path, looks nicer
      if (grid[selectedR][selectedC]!=Empty && !parameters.isDefined("noimages"))
      {
        g.setColor(colors[grid[selectedR][selectedC]-1]);
        g.fillRect(selectedC,selectedR,1,1);
      }      
    }

    //draw pieces and walls
    for (int r=0; r<N; r++)
      for (int c=0; c<N; c++)
      {
        if (grid[r][c]>0)
        {
          drawPiece(g,r,c);
        }
      }

    // draw selected squares.
    if (selectedR!=-1)
    {
      g.setColor(Color.blue);
      g.setStroke(new BasicStroke(0.05f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.drawRect(selectedC,selectedR,1,1);
    }
    if (selectedR2!=-1)
    {
      g.setColor(Color.blue);
      g.setStroke(new BasicStroke(0.05f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.drawRect(selectedC2,selectedR2,1,1);
    }

    //draw grid lines
    g.setColor(Color.black);
    g.setStroke(new BasicStroke(0.005f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (int i=0; i<=N; i++)
    {
      g.drawLine(i,0,i,N);
      g.drawLine(0,i,N,i);
    }
    if (errorR!=-1 && errorR2==-1)
    {
      g.setColor(Color.red);
      g.setStroke(new BasicStroke(0.05f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.drawRect(errorC,errorR,1,1);
      g.drawRect(errorC2,errorR2,1,1);
    }
  }


  private double shorten(double a)
  {
    return (double)Math.round(a * 1000.0) / 1000.0;
  }


  private Image loadImage(String name)
  {
    try{
      Image im=ImageIO.read(getClass().getResourceAsStream(name));
      return im;
    } catch (Exception e) {
      return null;
    }
  }


  private void init()
  {
    countErrors();
    score=numErrors*N;

    if (hasVis())
    {
      if (!parameters.isDefined("noimages"))
      {
        Queen=loadImage("images/queen.png");
      }

      setDefaultDelay(1000);    //this needs to be first

      setContentRect(0, 0, N, N);
      setInfoMaxDimension(20, 11);

      addInfo("Seed", seed);
      addInfo("N", N);
      addInfo("C", C);
      addInfo("P", shorten(P));

      addInfoBreak();
      addInfo("Time", 0);
      addInfo("Moves", 0);
      addInfo("Path Penalty", 0);
      addInfo("Total Penalty", 0);
      addInfo("Errors", numErrors);      
      addInfo("Score", score);        
      update();
    }
  }
        

  public static void main(String[] args) {
      new MarathonController().run(args);
  }
}
