package com.limegroup.gnutella.util;

/**
 * An approximate string matcher.  Two strings are considered
 * "approximately equal" if one can be transformed into the other
 * through some series of inserts, deletes, and substitutions.
 * Typical use:
 *
 * <pre>
 * a="Dr. Dre, Eminem - Forgot About Dre";
 * b="Dr. Dre ft EMINEM-Forgot About Dre";
 * matcher=new ApproximateMatcher(a, b);
 * float f1=matcher.compare;    //f1==70.5 now
 * matcher.ignoreCase(true)
 * float f2=matcher.compare;    //f2==85.3 now
 * </pre>
 *
 * This implementation is based on the ApproximateStringMatch class
 * written by C. Haberl of Software Systems, version 1.4, dated 07.06.1998.
 * It was downloaded from http://www.software-systems.de/approxj.zip.
 */
public class ApproximateMatcher
{
    private String  m_sPattern;
    private String  m_sText;

    private int[]   m_aCurrentCol;
    private int[]   m_aLeftCol;

    private boolean m_bIgnoreCase;
    private boolean m_bPartial;

    /// you can play with these three constants (>= 1)
    static final int COST_SUBSTITUTION  = 1;    // 3
    static final int COST_INSERTION     = 1;    // 1
    static final int COST_DELETION      = 1;    // 6

    /** 
     * Creates a new matcher to match sPattern against sText.  The default
     * matcher is case-sensitive and does not do partial matches.
     */
    public ApproximateMatcher( String sPattern, String sText )
    {
        m_sPattern  = sPattern;
        m_sText     = sText;
    }


    private void init( )
    {

        m_aCurrentCol   = new int[ m_sPattern.length() + 1 ];
        m_aLeftCol      = new int[ m_sPattern.length() + 1 ];

        /// initialize the first column
        for( int i = 0; i <= m_sPattern.length(); i++ )
            m_aCurrentCol[ i ] = i;

    }

    /** 
     * Returns the match of the pattern and the text as a percentage,
     * from 0.f (no match) to 100.f (perfect match), inclusive.
     */
    public float compare( ) 
    {

        int[] aTmp;
        int j, i;
        int nLengthToCompare;


        if( m_sPattern.length() == 0 || m_sText.length() == 0 )
            return( 0 );

        if( compareString( m_sPattern, m_sText ) )
            return( 100f );  /// strings are 100% equal

        /// initialization
        init();

        if( m_bPartial )
            nLengthToCompare = Math.min(m_sPattern.length(),
                                        m_sText.length() );
        else
            nLengthToCompare = m_sText.length();
        
        for( j = 0; j < nLengthToCompare; j++ )
        {

            aTmp            = m_aCurrentCol;
            m_aCurrentCol   = m_aLeftCol;
            m_aLeftCol      = aTmp;

            m_aCurrentCol[ 0 ] = j + 1;

            for( i = 0; i < m_sPattern.length(); i++ )
            {

                m_aCurrentCol[ i + 1 ] = min3( 
                    compareChar( m_sPattern.charAt( i ), m_sText.charAt( j ) ) ?
                        m_aLeftCol[ i ] : m_aLeftCol[ i ] + COST_SUBSTITUTION,
                    m_aLeftCol[ i + 1 ] + COST_INSERTION,
                    m_aCurrentCol[ i ] + COST_DELETION );
            }

        }

        return( ( 1.0f - (float) m_aCurrentCol[ m_sPattern.length() ] /
            (float)Math.max(nLengthToCompare, m_sPattern.length()) ) * 100.0f );


    }


    private boolean compareChar( char cPattern, char cText )
    {

        
        if( m_bIgnoreCase )
        {
            cPattern    = Character.toUpperCase( cPattern );
            cText       = Character.toUpperCase( cText    );
        }
        

        return( cPattern == cText );

    }

    private boolean compareString( String sFirst, String sSecond )
    {

        if( m_bIgnoreCase )
            return( sFirst.equalsIgnoreCase( sSecond ) );
        else
            return( sFirst.equals( sSecond ));

    }

    private int min3( int n1, int n2, int n3 )
    {
        return( Math.min( n1, Math.min( n2, n3 ) ) );
    }

    /**
     * Sets whether this should ignore case.  If bIgnore is true,
     * case will be ignored in matching; otherwise it will not.
     *
     * @modifies this
     */
    public void setIgnoreCase( boolean bIgnore )
    {
        m_bIgnoreCase = bIgnore;
    }

    /** 
     * Sets whether this should do partial matches.  If bPartial
     * is true, compares sPattern.length() characters within sText.
     * Otherwise compares sPattern with complete sText.
     *
     * @modifies this
     */
    public void setPartialMatch( boolean bPartial )
    {
        m_bPartial = bPartial;
    }
}

