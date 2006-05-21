<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
<HTML>
<BODY>
<FORM>
	<TABLE id="folderListTable">
	<xsl:for-each select="folders/folder">
    <tr id="{id}">
		<xsl:choose>
	        <xsl:when test="type='Public'">
				<TD><img src="/icons/Folder.gif" alt="P" border="0" /></TD>
			</xsl:when>
			 <xsl:when test="type='Private'">
				<TD><img src="/icons/Folder_yellow.gif" alt="S" border="0" /></TD>
			</xsl:when>
		</xsl:choose>
	<TD><A HREF="#" class="link" onclick="showFolder('{id}', '/')"><xsl:value-of select="name" /></A></TD>	
	<td width="10px"></td>
	<TD><INPUT class="smallbutton" TYPE="button" ID="leavebutton" name="leavebutton" value="Leave" onclick="leave('{name}', '{id}')" /></TD>
    </tr>
    </xsl:for-each>
</TABLE>
</FORM>
</BODY>
</HTML>
</xsl:template>

</xsl:stylesheet>