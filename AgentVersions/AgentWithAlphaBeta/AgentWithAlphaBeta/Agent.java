package AgentWithAlphaBeta;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;


public class Agent
{
	Side side;

	Kalah kalah;

	int holes;

	int moveCount = 0;

	boolean first;

	boolean maySwap;

	int depthToSearch;


	public Agent(int holes, int seeds)
	{
		this.holes = holes;
		kalah = new Kalah(new Board(holes, seeds));
	}

	public Agent(int holes, int seeds, int depth)
	{
		this.holes = holes;
		kalah = new Kalah(new Board(holes, seeds));
		depthToSearch = depth;
	}

	//EXAMPLE OF EVALUATION FUNCTION, COPIED FROM THE REFAGENT
	//TO DO: CREATE OUR OWN EVALUATION FUNCTION USING THE HEURISTICS
	private int evaluate(Board b)
	{
		int ourSeeds = b.getSeedsInStore(side);
		int oppSeeds = b.getSeedsInStore(side.opposite());

		for (int i = 1; i <= holes; i++)
		{
			ourSeeds += b.getSeeds(side, i);
			oppSeeds += b.getSeeds(side.opposite(), i);
		}

		return ourSeeds - oppSeeds;
	}


	public int defendSeeds(Board board, Side ourSide)
	{

      int amountStealable = 0;
      int pos1 = 0, pos2 = 0, pos3 = 0;
      // find opponent pots where he has 0 seeds, but on the other side of the board we have seeds
      List<Integer> candidatePots = new LinkedList<Integer>();

      int pos = 0;
      for(int i=1; i<=board.getNoOfHoles(); i++)
      {
          // pos < 7 to ensure that we're not looking at a scoring well
          if (pos < board.getNoOfHoles() &&
                  board.getSeeds(ourSide.opposite(), i) == 0 && board.getSeeds(ourSide, 7-pos) != 0)
              candidatePots.add(pos);
          pos++;
      }

      // find how many can be stolen by sowing from a pot and dropping the last stone in the same pot
      pos = 0;
      for (int i=1; i<=board.getNoOfHoles(); i++)
          if(board.getSeeds(ourSide.opposite(), i) == 2 * board.getNoOfHoles() + 1)
          {   
            pos1 += board.getSeeds(ourSide, 7-pos++) + 1;
            break;
          }

      // find how many of those pots can be reached by the opponent on his next turn
      for(int index : candidatePots)
      {
          // by sowing from pot with index smaller than empty pot
          pos = 0;
          for(int i=1; i<=board.getNoOfHoles(); i++)
              if (/*pos < index &&*/ board.getSeeds(ourSide.opposite(), i) == index - pos++ && board.getSeeds(ourSide.opposite(), i) != 0)
                  if(pos2 < board.getSeeds(ourSide, 7-index))
                    pos2 = board.getSeeds(ourSide, 7-index);

          // by sowing from pot with index greater than empty pot
          pos = 0;
          for(int i=1; i<=board.getNoOfHoles(); i++)
              if(/*pos > index && */board.getSeeds(ourSide.opposite(), i) == 2 * board.getNoOfHoles() + 1 - (pos++ - index) )
              {
                if(pos3 <  board.getSeeds(ourSide, 7-index) + 1)
                  pos3 =  board.getSeeds(ourSide, 7-index) + 1;
              }
      }

      amountStealable = Math.max(pos1, Math.max(pos2, pos3));
      return amountStealable;
  }

	public int scoringWellDiff(Board board, Side ourSide) {
    return board.getSeedsInStore(ourSide) - board.getSeedsInStore(ourSide.opposite());
  }

	public int clusterTowardsScoringWell(Board board, Side side) {
        //Grid grid = board.getPlayersGrid(side);

        int n = 0;
        for (int i = 1; i <= board.getNoOfHoles(); ++i) {
            //Pot pot = grid.getPots()[i];
            if (board.getSeeds(side, i) == 0) continue;
            n += board.getSeeds(side, i) * i;
        }
        return n;
    }

	public int evaluateLeafNodes(Board b) 
	{
		int w1 = 3;
  		int w2 = 5;
 		int w3 = 20;

		int evaluation =  scoringWellDiff(b, side) * w1
                          - defendSeeds(b, side)   / w2
                           + clusterTowardsScoringWell(b, side) / w3;
        return evaluation;
    }
	// Params: the depth of the search, the current board, whose turn it is, alpha, beta
	// Return: int array containing max/minEval, alpha, beta
	// TO DO: For alpha-beta pruning, add alpha and beta arguments of type int
	private int[] minimax(int depth, Board board, boolean maximizingPlayer, int alpha, int beta) {
		//Search to the given depth or leaf node reached
		if (depth == 0 || kalah.gameOver(board)) {
			int v = evaluateLeafNodes(board);
		//	System.err.println("Evaluated value: " + v);
			return new int[] {v, alpha, beta};
		}

		if (maximizingPlayer) {

			int maxEval = Integer.MIN_VALUE;
			for(int i = 1; i <= holes; i++) {

				Move move = new Move(side,i);

				if (kalah.isLegalMove(board, move)) {
					//Clone the current state of the board
					Board lastBoard = board;
					try  {
						lastBoard = board.clone();
					}
					catch(CloneNotSupportedException e){
						System.err.println(e.getMessage());
					}

					Side nextTurn = Kalah.makeMove(board, move);
					//System.err.println("Board after max move "+i+": "+board.toString());
					if(nextTurn == side)
						maximizingPlayer = true;
					else
						maximizingPlayer = false;

					int eval = minimax(depth-1, board, maximizingPlayer, alpha, beta)[0];

					//Undo the last move
					board = lastBoard;

					maxEval = Math.max(maxEval, eval);
					alpha = Math.max(alpha, eval);
				//	System.err.println("alpha: " + alpha);
					if (beta <= alpha) {
				//		System.err.println("Pruning, alpha: " + alpha + ", beta: " + beta);
						break;
					}

				}
			}
			return new int[] {maxEval, alpha, beta};
		}

		else {
			int minEval = Integer.MAX_VALUE;

			for(int i = 1; i <= holes; i++) {

				Move move = new Move(side.opposite(),i);

				if (kalah.isLegalMove(board, move)) {
				//	System.err.println("Board before min move: " + board);
					//Clone the current state of the board
					Board lastBoard = board;
					try  {
						lastBoard = board.clone();
					}
					catch(CloneNotSupportedException e){
						System.err.println(e.getMessage());
						 }
					Side nextTurn = Kalah.makeMove(board, move);
					//System.err.println("Board after min move "+i+": "+board.toString());
					if(nextTurn == side)
						maximizingPlayer = true;
					else
						maximizingPlayer = false;
					//System.err.println("Board after min move: " + board);
					//System.err.println("Is it our turn now? " + maximizingPlayer);

					int eval = minimax(depth-1, board, maximizingPlayer, alpha, beta)[0];

					//Undo the last move
					board = lastBoard;
					minEval = Math.min(minEval, eval);

					beta = Math.min(beta, eval);
				//	System.err.println("beta: " + beta);
					if (beta <= alpha) {
				//		System.err.println("Pruning, alpha: " + alpha + ", beta: " + beta);
						break;
					}
				}
			}
			return new int[] {minEval, alpha, beta};
		}
	}

	//TO DO: MODIFY THE METHOD SO THAT IT WOULD CHOOSE THE BEST NEW MOVE FIRST
	//BY CALLING EVALUATION FUNCTION ON ALL POSSIBLE MOVES FIRST AND THEN
	//SORT THEM AND CALL THE MINIMAX IN THE SORTED ORDER
	//Finds the next best move to make
	private int nextMove()
	{
		int bestMove = 0;
		int bestHeuristics = Integer.MIN_VALUE;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		boolean ourTurn = false;

		//Go through all the possible moves
		for (int i = 1; i <= holes; i++) {

			Move move = new Move(side,i);

			if (kalah.isLegalMove(move)) {

				Board board = new Board(kalah.getBoard());
				Side nextTurn = Kalah.makeMove(board, move);
				//Check whose turn it is next
				if(nextTurn == side)
					ourTurn = true;
				else
					ourTurn = false;
				if(first && moveCount ==1)
					ourTurn = false;
				//System.err.printf("Board after agent move %d: ",i);
				//System.err.println(board);
				//System.err.println("Is it our turn now? " + ourTurn);

				int[] results = minimax(depthToSearch, board,ourTurn, alpha, beta);
				int heuristics = results[0];
				alpha = results[1];
				alpha = Math.max(alpha, heuristics);

				//Check if this move is better than previous best one
				if (heuristics > bestHeuristics) {
					bestMove = i;
					bestHeuristics = heuristics;
				}
			}
		}
		return bestMove;
	}

	//Checks whether to perform a swap or a normal move
	//Returns -1 if swap, else number of the best move
	private int toSwap()
	{
		System.err.println("Checking whether to swap");
		int bestMove = 0;
		int swapEvaluation = Integer.MIN_VALUE;
		int noSwapEvaluation = Integer.MIN_VALUE;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		boolean ourTurn = false;

		//Calculate evaluation for no swap
		//Go through all the possible moves
		System.err.println("Checking evaluation for no swapping " + kalah.getBoard());
		for (int i = 1; i <= holes; i++) {

			Move move = new Move(side,i);

			if (kalah.isLegalMove(move)) {
				Board board = new Board(kalah.getBoard());
				Side nextTurn = Kalah.makeMove(board, move);
			//	System.err.println("nextTurn:" + nextTurn+ " side: "+side);
				//Check whose turn it is next
				if(nextTurn == side)
					ourTurn = true;
				else
					ourTurn = false;
			//	System.err.printf("Board after agent move %d: ",i);
			//	System.err.println(board);
			//	System.err.println("Is it our turn now? " + ourTurn);
				int[] results = minimax(depthToSearch, board,ourTurn, alpha, beta);
				int heuristics = results[0];
				alpha = results[1];
				alpha = Math.max(alpha, heuristics);

				//Check if this move is better than previous best one
				if (heuristics > noSwapEvaluation) {
					bestMove = i;
					noSwapEvaluation = heuristics;
				}
			}
		}

		swap();
		alpha = Integer.MIN_VALUE;
		beta = Integer.MAX_VALUE;
		
		System.err.println("Checking evaluation for swapping " + kalah.getBoard());
		
		//Calculate evaluation for swap
		//Go through all the possible moves
		for (int i = 1; i <= holes; i++) {

			Move move = new Move(side.opposite(),i);

			if (kalah.isLegalMove(move)) {
				Board board = new Board(kalah.getBoard());
				Side nextTurn = Kalah.makeMove(board, move);
				//Check whose turn it is next
				if(nextTurn == side)
					ourTurn = true;
				else
					ourTurn = false;
				//System.err.printf("Board after not agent move %d: ",i);
				//System.err.println(board);
				//System.err.println("Is it our turn now? " + ourTurn);
				int[] results = minimax(depthToSearch, board, ourTurn, alpha, beta);
				int heuristics = results[0];
				beta = results[2];
				beta = Math.min(beta, heuristics);
				//Check if this move is better than previous best one
				if (heuristics > swapEvaluation) {
					swapEvaluation = heuristics;
				}
			}
		}
		swap();
		System.err.printf("Swap evaluation: %d, noSwapEvaluation: %d\n", swapEvaluation, noSwapEvaluation);
		if(swapEvaluation > noSwapEvaluation)
			return -1;
		else
			return bestMove;
	}

	//Swap sides if either player chooses to play swap
	private void swap()
	{
		side = side.opposite();
	}

	public void start()
	{
		try {
			//Puts the System.err prints to a err.txt file for debugging
			File file = new File("errAlphaBeta.txt");
			FileOutputStream fos = new FileOutputStream(file);
			System.setErr(new PrintStream(fos));

			//To store messages from the engine
			String msg;

			//If the agent isn't the starting player it can swap sides
			maySwap = false;
			if(depthToSearch == 0)
				depthToSearch = 7;
			System.err.println("Agent started");
			System.err.println("Depth to search: "+depthToSearch);
			//Make a move
			while(true) {
				//Get message from the engine
				msg = Main.recvMsg();
				try {
					MsgType mt = Protocol.getMessageType(msg);
					switch(mt) {

						case START:
						System.err.println("A start");
						//Check which side the agent is playing
						first = Protocol.interpretStartMsg(msg);
						//Make the first move, now just plays hole 1
						if (first) {
							side = Side.SOUTH;
							moveCount++;
							int move = nextMove();
							Main.sendMsg(Protocol.createMoveMsg(move));
						}
						else {
							side = Side.NORTH;
							maySwap = true;
						}
						System.err.println("Starting player? " + first);
						break;

						case STATE:
						System.err.println("A state");
						Protocol.MoveTurn moveTurn = Protocol.interpretStateMsg(msg, kalah.getBoard());
						System.err.println("This was the move: " + moveTurn.move);
						System.err.println("Is the game over? " + moveTurn.end);
						System.err.println("Is it our turn again? " + moveTurn.again);
						//If opponent swapped sides
						if (moveTurn.move == -1) {
							swap();
						}
						//If it's our turn again and the game hasn't ended
						if ((moveTurn.again) && (!moveTurn.end)) {

							msg = null;
							int move = 0;

							//Check whether to swap or not
							if (maySwap) {
								moveCount++;
								int swapMove = toSwap();

								if (swapMove == -1) {
									System.err.println("Swapping");
									swap();
									msg = Protocol.createSwapMsg();
								}

								else
									move = swapMove;
							}
							else
							{
								moveCount++;
								move = nextMove();
							}
							maySwap = false;

							if (msg == null)
								msg = Protocol.createMoveMsg(move);
							Main.sendMsg(msg);
						}

						System.err.print("The board:\n" + kalah.getBoard());
						break;

						case END:
						System.err.println("An end");
						return;

					}
				}
				catch (InvalidMessageException e) {
					System.err.println(e.getMessage());
				}
			}
		}
		catch (IOException e) {
			System.err.println("This shouldn't happen " + e.getMessage());
		}
	}
}
