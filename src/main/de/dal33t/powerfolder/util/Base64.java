
package de.dal33t.powerfolder.util;


/* @author Robert Harder
 */

public class Base64 {
    
    /** No options specified. Value is zero. */
    public final static int NO_OPTIONS = 0;
    
    /** Specify encoding. */
    public final static int ENCODE = 1;
    
    /** Specify that data should be gzip-compressed. */
    public final static int GZIP = 2;
    
    /** Don't break lines when encoding (violates strict Base64 specification) */
    public final static int DONT_BREAK_LINES = 8;
    
    
    /** Maximum line length (76) of Base64 output. */
    private final static int MAX_LINE_LENGTH = 76;
    
    
    /** The equals sign (=) as a byte. */
    private final static byte EQUALS_SIGN = (byte)'=';
    
    
    /** The new line character (\n) as a byte. */
    private final static byte NEW_LINE = (byte)'\n';
    
    
    /** Preferred encoding. */
    private final static String PREFERRED_ENCODING = "UTF-8";
    
    
    /** The 64 valid Base64 values. */
    private final static byte[] ALPHABET;
    private final static byte[] _NATIVE_ALPHABET = 
    {
        (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G',
        (byte)'H', (byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N',
        (byte)'O', (byte)'P', (byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U', 
        (byte)'V', (byte)'W', (byte)'X', (byte)'Y', (byte)'Z',
        (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f', (byte)'g',
        (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
        (byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u', 
        (byte)'v', (byte)'w', (byte)'x', (byte)'y', (byte)'z',
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', 
        (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'+', (byte)'/'
    };
    
    /** Determine which ALPHABET to use. */
    static
    {
        byte[] __bytes;
        try
        {
            __bytes = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes( PREFERRED_ENCODING );
        }   // end try
        catch (java.io.UnsupportedEncodingException use)
        {
            __bytes = _NATIVE_ALPHABET; // Fall back to native encoding
        }   // end catch
        ALPHABET = __bytes;
    }   // end static
    
        
    //private final static byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding
    //private final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding

    
    private Base64(){}
    
    
    /**
     * Encodes up to the first three bytes of array <var>threeBytes</var>
     * and returns a four-byte array in Base64 notation.
     * The actual number of significant bytes in your array is
     * given by <var>numSigBytes</var>.
     * The array <var>threeBytes</var> needs only be as big as
     * <var>numSigBytes</var>.
     * Code can reuse a byte array by passing a four-byte array as <var>b4</var>.
     *
     * @param b4 A reusable byte array to reduce array instantiation
     * @param threeBytes the array to convert
     * @param numSigBytes the number of significant bytes in your array
     * @return four byte array in Base64 notation.
     */
    private static byte[] encode3to4( byte[] b4, byte[] threeBytes, int numSigBytes )
    {
        encode3to4( threeBytes, 0, numSigBytes, b4, 0 );
        return b4;
    }   
    
    /**
     * Encodes up to three bytes of the array <var>source</var>
     * and writes the resulting four Base64 bytes to <var>destination</var>.
     * The source and destination arrays can be manipulated
     * anywhere along their length by specifying 
     * <var>srcOffset</var> and <var>destOffset</var>.
     * This method does not check to make sure your arrays
     * are large enough to accomodate <var>srcOffset</var> + 3 for
     * the <var>source</var> array or <var>destOffset</var> + 4 for
     * the <var>destination</var> array.
     * The actual number of significant bytes in your array is
     * given by <var>numSigBytes</var>.
     *
     * @param source the array to convert
     * @param srcOffset the index where conversion begins
     * @param numSigBytes the number of significant bytes in your array
     * @param destination the array to hold the conversion
     * @param destOffset the index where output will be put
     * @return the <var>destination</var> array
     */
    private static byte[] encode3to4( 
     byte[] source, int srcOffset, int numSigBytes,
     byte[] destination, int destOffset )
    {
        //           1         2         3  
        // 01234567890123456789012345678901 Bit position
        // --------000000001111111122222222 Array position from threeBytes
        // --------|    ||    ||    ||    | Six bit groups to index ALPHABET
        //          >>18  >>12  >> 6  >> 0  Right shift necessary
        //                0x3f  0x3f  0x3f  Additional AND
        
        // Create buffer with zero-padding if there are only one or two
        // significant bytes passed in the array.
        // We have to shift left 24 in order to flush out the 1's that appear
        // when Java treats a value as negative that is cast from a byte to an int.
        int inBuff =   ( numSigBytes > 0 ? ((source[ srcOffset     ] << 24) >>>  8) : 0 )
                     | ( numSigBytes > 1 ? ((source[ srcOffset + 1 ] << 24) >>> 16) : 0 )
                     | ( numSigBytes > 2 ? ((source[ srcOffset + 2 ] << 24) >>> 24) : 0 );

        switch( numSigBytes )
        {
            case 3:
                destination[ destOffset     ] = ALPHABET[ (inBuff >>> 18)        ];
                destination[ destOffset + 1 ] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
                destination[ destOffset + 2 ] = ALPHABET[ (inBuff >>>  6) & 0x3f ];
                destination[ destOffset + 3 ] = ALPHABET[ (inBuff       ) & 0x3f ];
                return destination;
                
            case 2:
                destination[ destOffset     ] = ALPHABET[ (inBuff >>> 18)        ];
                destination[ destOffset + 1 ] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
                destination[ destOffset + 2 ] = ALPHABET[ (inBuff >>>  6) & 0x3f ];
                destination[ destOffset + 3 ] = EQUALS_SIGN;
                return destination;
                
            case 1:
                destination[ destOffset     ] = ALPHABET[ (inBuff >>> 18)        ];
                destination[ destOffset + 1 ] = ALPHABET[ (inBuff >>> 12) & 0x3f ];
                destination[ destOffset + 2 ] = EQUALS_SIGN;
                destination[ destOffset + 3 ] = EQUALS_SIGN;
                return destination;
                
            default:
                return destination;
        }   
    }       
    
    
    /**
     * Encodes a byte array into Base64 notation.
     * Does not GZip-compress data.
     *
     * @param source 
     */
    public static String encodeBytes( byte[] source )
    {
        return encodeBytes( source, 0, source.length, NO_OPTIONS );
    }   // end encodeBytes
    


    /**
     * Encodes a byte array into Base64 notation.
     * <p>
     * Valid options:<pre>
     *   GZIP: gzip-compresses object before encoding it.
     *   DONT_BREAK_LINES: don't break lines at 76 characters
     *     <i>Note: Technically, this makes your encoding non-compliant.</i>
     * </pre>
     * <p>
     * Example: <code>encodeBytes( myData, Base64.GZIP )</code> or
     * <p>
     * Example: <code>encodeBytes( myData, Base64.GZIP | Base64.DONT_BREAK_LINES )</code>
     *
     *
     * @param source The data to convert
     * @param off Offset in array where conversion should begin
     * @param len Length of data to convert
     * @param options Specified options
     */
    public static String encodeBytes( byte[] source, int off, int len, int options )
    {
        // Isolate options
        int dontBreakLines = ( options & DONT_BREAK_LINES );
        int gzip           = ( options & GZIP   );
        
        // Compress?
        if( gzip == GZIP )
        {
            java.io.ByteArrayOutputStream  baos  = null;
            java.util.zip.GZIPOutputStream gzos  = null;
            Base64.OutputStream            b64os = null;
            
    
            try
            {
                // GZip -> Base64 -> ByteArray
                baos = new java.io.ByteArrayOutputStream();
                b64os = new Base64.OutputStream( baos, ENCODE | dontBreakLines );
                gzos  = new java.util.zip.GZIPOutputStream( b64os ); 
            
                gzos.write( source, off, len );
                gzos.close();
            }   // end try
            catch( java.io.IOException e )
            {
                e.printStackTrace();
                return null;
            }   // end catch
            finally
            {
                try{ gzos.close();  } catch( Exception e ){}
                try{ b64os.close(); } catch( Exception e ){}
                try{ baos.close();  } catch( Exception e ){}
            }   
            // Return value according to relevant encoding.
            try
            {
                return new String( baos.toByteArray(), PREFERRED_ENCODING );
            }   
            catch (java.io.UnsupportedEncodingException uue)
            {
                return new String( baos.toByteArray() );
            }   
         }   
        
        // Else, don't compress. Better not to use streams at all then.
        // Convert option to boolean in way that code likes it.
        boolean breakLines = dontBreakLines == 0;
          
        int    len43   = len * 4 / 3;
        byte[] outBuff = new byte[   ( len43 )                      // Main 4:3
                                   + ( (len % 3) > 0 ? 4 : 0 )      // Account for padding
                                   + (breakLines ? ( len43 / MAX_LINE_LENGTH ) : 0) ]; // New lines      
        int d = 0;
        int e = 0;
        int len2 = len - 2;
        int lineLength = 0;
        for( ; d < len2; d+=3, e+=4 )
        {
            encode3to4( source, d+off, 3, outBuff, e );
            lineLength += 4;
            if( breakLines && lineLength == MAX_LINE_LENGTH )
            {   
                outBuff[e+4] = NEW_LINE;
                e++;
                lineLength = 0;
            }   
        }  
        if( d < len )
        {
            encode3to4( source, d+off, len - d, outBuff, e );
            e += 4;
        }   
            
        // Return value according to relevant encoding.
        try
        {
            return new String( outBuff, 0, e, PREFERRED_ENCODING );
        }   
        catch (java.io.UnsupportedEncodingException uue)
        {
            return new String( outBuff, 0, e );
        }   
    }           
 
    
    /**
     * Writes data to another <tt> java.io.OutputStream</tt>, given in the constructor,
     * and encode/decode to/from Base64 notation on the fly.
     */
    public static class OutputStream extends java.io.FilterOutputStream
    {
        private boolean encode;
        private int     position;
        private byte[]  buffer;
        private int     bufferLength;
        private int     lineLength;
        private boolean breakLines;
        private byte[]  b4; // Scratch used in a few places
        private boolean suspendEncoding;
        
        /**
         * Constructs a {@link Base64.OutputStream} in ENCODE mode.
         *
         * @param out the <tt>java.io.OutputStream</tt> to which data will be written.
         * @since 1.3
         */
        public OutputStream( java.io.OutputStream out )
        {   
            this( out, ENCODE );
        }   // end constructor
        
        
        /**
         * Constructs a {@link Base64.OutputStream} in
         * either ENCODE or DECODE mode.
         * <p>
         * Valid options:<pre>
         *   ENCODE or DECODE: Encode or Decode as data is read.
         *   DONT_BREAK_LINES: don't break lines at 76 characters
         *     (only meaningful when encoding)
         *     <i>Note: Technically, this makes your encoding non-compliant.</i>
         * </pre>
         * <p>
         * Example: <code>new Base64.OutputStream( out, Base64.ENCODE )</code>
         *
         * @param out the <tt>java.io.OutputStream</tt> to which data will be written.
         * @param options Specified options.
         */
        public OutputStream( java.io.OutputStream out, int options )
        {   
            super( out );
            this.breakLines   = (options & DONT_BREAK_LINES) != DONT_BREAK_LINES;
            this.encode       = (options & ENCODE) == ENCODE;
            this.bufferLength = encode ? 3 : 4;
            this.buffer       = new byte[ bufferLength ];
            this.position     = 0;
            this.lineLength   = 0;
            this.suspendEncoding = false;
            this.b4           = new byte[4];
        }           
        
        /**
         * Writes the byte to the output stream after
         * converting to/from Base64 notation.
         * When encoding, bytes are buffered three
         * at a time before the output stream actually
         * gets a write() call.
         * When decoding, bytes are buffered four
         * at a time.
         *
         * @param theByte the byte to write
         */
        public void write(int theByte) throws java.io.IOException
        {
            // Encoding suspended?
            if( suspendEncoding )
            {
                super.out.write( theByte );
                return;
            }   // end if: supsended
            
            // Encode?
            if( encode )
            {
                buffer[ position++ ] = (byte)theByte;
                if( position >= bufferLength )  // Enough to encode.
                {
                    out.write( encode3to4( b4, buffer, bufferLength ) );

                    lineLength += 4;
                    if( breakLines && lineLength >= MAX_LINE_LENGTH )
                    {
                        out.write( NEW_LINE );
                        lineLength = 0;
                    }   

                    position = 0;
                }   
            }
        }   
        
        
        
        /**
         * Calls {@link #write(int)} repeatedly until <var>len</var> 
         * bytes are written.
         *
         * @param theBytes array from which to read bytes
         * @param off offset for array
         * @param len max number of bytes to read into array
         */
        public void write( byte[] theBytes, int off, int len ) throws java.io.IOException
        {
            // Encoding suspended?
            if( suspendEncoding )
            {
                super.out.write( theBytes, off, len );
                return;
            }   
            
            for( int i = 0; i < len; i++ )
            {
                write( theBytes[ off + i ] );
            }   
            
        }   
        
        
        
        /**
         * This pads the buffer without closing the stream.
         */
        public void flushBase64() throws java.io.IOException 
        {
            if( position > 0 )
            {
                if( encode )
                {
                    out.write( encode3to4( b4, buffer, position ) );
                    position = 0;
                }   
                else
                {
                    throw new java.io.IOException( "Base64 input not properly padded." );
                }   
            }   

        }   

        
        /** 
         * Flushes and closes (I think, in the superclass) the stream. 
         *
         */
        public void close() throws java.io.IOException
        {
            // 1. Ensure that pending characters are written
            flushBase64();

            // 2. Actually close the stream
            // Base class both flushes and closes.
            super.close();
            
            buffer = null;
            out    = null;
        }   
        
        
        
        /**
         * Suspends encoding of the stream.
         * May be helpful if you need to embed a piece of
         * base640-encoded data in a stream.
         *
         */
        public void suspendEncoding() throws java.io.IOException 
        {
            flushBase64();
            this.suspendEncoding = true;
        }   
        
        /**
         * Resumes encoding of the stream.
         * May be helpful if you need to embed a piece of
         * base640-encoded data in a stream.
         *
         */
        public void resumeEncoding()
        {
            this.suspendEncoding = false;
        }           
    }   
    
}   
