package org.limewire.ui.swing.tictactoe;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.ui.swing.tictactoe.TicTacToeBoard;
import org.limewire.ui.swing.tictactoe.TicTacToeJudge;
import org.limewire.ui.swing.tictactoe.TicTacToeWinner;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;

/**
 * Tests the various ways a Tic Tac Toe game could be won, along with
 * testing that draws and game not decided is returned.
 * <p>
 * <ol>
 * <li>Extend from <code>BaseTestCase</code>.
 * <li>Add a <code>suite</code> method which builds the test suite using.
 * <li>Precede each method you want to be called by the suite with 'test'.
 * <li>Use asserts.
 * <li>Use mocks when appropriate.
 * <p>
 * Additionally, this test class has two methods to show how to use mocks
 * in test cases. In reality, mocks aren't necessary for this case because
 * creating a <code>TicTacToeBoard</code> instance can be done using an initial 
 * constructor. However, mocks are included for demonstration.
 */
public class TicTacToeJudgeTest extends BaseTestCase {
    
    public TicTacToeJudgeTest(String name) {
        super(name);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(TicTacToeJudgeTest.class);
    }
    /**
     * @throws Exception if any error occurs.
     */
    public void testXAcross() throws Exception  {
        TicTacToeJudge test = new TicTacToeJudge();
        TicTacToeBoard x1 = new TicTacToeBoard('x', 'x', 'x', 
                                               'o', 'o', 'x', 
                                               'o', 'o', 'e');

        assertEquals(test.result(x1), TicTacToeWinner.X_ACROSS_1);

        TicTacToeBoard x2 = new TicTacToeBoard('o', 'x', 'x', 
                                               'x', 'x', 'x', 
                                               'o', 'o', 'e');

        assertEquals(test.result(x2), TicTacToeWinner.X_ACROSS_2);
        
        TicTacToeBoard x3 = new TicTacToeBoard('o', 'x', 'o', 
                                                'o', 'o', 'x', 
                                                'x', 'x', 'x');
        assertEquals(test.result(x3), TicTacToeWinner.X_ACROSS_3);

    }

    public void testOAcross() throws Exception  {
        TicTacToeJudge test = new TicTacToeJudge();
        TicTacToeBoard o1 = new TicTacToeBoard('o', 'o', 'o', 
                                               'o', 'x', 'x', 
                                               'x', 'o', 'e');

        assertEquals(test.result(o1), TicTacToeWinner.O_ACROSS_1);

        TicTacToeBoard o2 = new TicTacToeBoard('o', 'x', 'x', 
                                               'o', 'o', 'o', 
                                               'x', 'o', 'e');

        assertEquals(test.result(o2), TicTacToeWinner.O_ACROSS_2);
        
        TicTacToeBoard o3 = new TicTacToeBoard('o', 'x', 'o', 
                                               'x', 'o', 'x', 
                                               'o', 'o', 'o');
        assertEquals(test.result(o3), TicTacToeWinner.O_ACROSS_3);

    }
    public void testXDown() throws Exception  {
        TicTacToeJudge test = new TicTacToeJudge();
        TicTacToeBoard x1 = new TicTacToeBoard('x', 'x', 'o', 
                                               'x', 'o', 'x', 
                                               'x', 'o', 'e');

        assertEquals(test.result(x1), TicTacToeWinner.X_DOWN_1);

        TicTacToeBoard x2 = new TicTacToeBoard('o', 'x', 'o', 
                                               'o', 'x', 'o', 
                                               'x', 'x', 'e');

        assertEquals(test.result(x2), TicTacToeWinner.X_DOWN_2);
        
        TicTacToeBoard x3 = new TicTacToeBoard('o', 'x', 'x', 
                                               'x', 'o', 'x', 
                                               'o', 'o', 'x');
        assertEquals(test.result(x3), TicTacToeWinner.X_DOWN_3);

    }

    public void testODown() throws Exception  {
        TicTacToeJudge test = new TicTacToeJudge();
        TicTacToeBoard o1 = new TicTacToeBoard('o', 'x', 'o', 
                                               'o', 'x', 'x', 
                                               'o', 'o', 'e');

        assertEquals(test.result(o1), TicTacToeWinner.O_DOWN_1);

        TicTacToeBoard o2 = new TicTacToeBoard('o', 'o', 'x', 
                                               'x', 'o', 'o', 
                                               'x', 'o', 'e');

        assertEquals(test.result(o2), TicTacToeWinner.O_DOWN_2);
        
        TicTacToeBoard o3 = new TicTacToeBoard('x', 'x', 'o', 
                                               'x', 'o', 'o', 
                                               'o', 'x', 'o');
        assertEquals(test.result(o3), TicTacToeWinner.O_DOWN_3);

    }

    public void testXDiagonal() throws Exception  {
        TicTacToeJudge test = new TicTacToeJudge();
        //negative slope
        TicTacToeBoard x1 = new TicTacToeBoard('x', 'x', 'o', 
                                               'o', 'x', 'x', 
                                               'o', 'o', 'x');

        assertEquals(test.result(x1), TicTacToeWinner.X_DIAG_1);

        //positive slope
        TicTacToeBoard x2 = new TicTacToeBoard('o', 'x', 'x', 
                                               'o', 'x', 'o', 
                                               'x', 'o', 'e');

        assertEquals(test.result(x2), TicTacToeWinner.X_DIAG_2);
                
    }

    public void testODiagonal() throws Exception  {
        TicTacToeJudge test = new TicTacToeJudge();
        //negative slope
        TicTacToeBoard o1 = new TicTacToeBoard('o', 'x', 'o', 
                                               'x', 'o', 'x', 
                                               'x', 'o', 'o');

        assertEquals(test.result(o1), TicTacToeWinner.O_DIAG_1);

        //positive slope
        TicTacToeBoard o2 = new TicTacToeBoard('x', 'x', 'o', 
                                               'x', 'o', 'o', 
                                               'o', 'x', 'e');

        assertEquals(test.result(o2), TicTacToeWinner.O_DIAG_2);
                
    }

    public void testDraw() throws Exception  {
        TicTacToeJudge test = new TicTacToeJudge();
        TicTacToeBoard draw = new TicTacToeBoard('o', 'x', 'o', 
                                                 'x', 'o', 'x', 
                                                 'x', 'o', 'x');
        
        assertEquals(test.result(draw), TicTacToeWinner.DRAW);

    }

    public void testNoWinnerYet() throws Exception  {
        
        TicTacToeJudge test = new TicTacToeJudge();
        TicTacToeBoard noOneYet = new TicTacToeBoard('o', 'x', 'o', 
                                                     'x', 'o', 'x', 
                                                     'x', 'o', 'e');
        
        assertEquals(test.result(noOneYet), TicTacToeWinner.NO_ONE_YET);


        //negative slope
        TicTacToeBoard x1 = new TicTacToeBoard('x', 'x', 'o', 
                                               'o', 'x', 'x', 
                                               'o', 'o', 'x');

        assertNotEquals(test.result(x1), TicTacToeWinner.NO_ONE_YET);

    }

    /**
     * You can mock the <code>TicTacToeBoard</code> in order to test whether
     * validating for a win is done correctly.
     * <p>
     * This mock sets up a board which has o winning through across 2.
     * <p>
     */
    public void testWithMocksAcross() throws Exception  {

        TicTacToeJudge test = new TicTacToeJudge(); 
        
        Mockery context = new Mockery();
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final TicTacToeBoard board = context.mock(TicTacToeBoard.class);
        context.checking(new Expectations() {
            {
                char [] mockBoard = {'e', 'e', 'x',
                                     'o', 'o', 'o',
                                     'x', 'x', 'o'
                        };
               
               allowing(board);
               will(returnValue(mockBoard));
                              
            }
        });
                
        assertEquals(test.result(board), TicTacToeWinner.O_ACROSS_2);
    }

    /**
     * Tests having a board without a winner and empty cells (marked as 'e')
     * returns as no one won yet (and not as a draw).
     */
    public void testWithMocksNotYet() throws Exception  {
        TicTacToeJudge test2 = new TicTacToeJudge(); 
        Mockery contextNotYet = new Mockery();
        contextNotYet = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final TicTacToeBoard board2 = contextNotYet.mock(TicTacToeBoard.class);

        contextNotYet.checking(new Expectations() {
            {
                char [] mockBoard2 = {'e', 'e', 'x',
                                     'o', 'o', 'x',
                                     'o', 'x', 'o'
                        };
               
               allowing(board2);
               will(returnValue(mockBoard2));
                              
            }
        });

                
        assertEquals(test2.result(board2), TicTacToeWinner.NO_ONE_YET);

    }

}
