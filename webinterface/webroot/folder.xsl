<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes"/>
<xsl:param name="sortOrder" select="ascending" />
<xsl:param name="sortColumn" select="name"/>
<xsl:param name="sortType" select="text"/>
<xsl:template match="/">
<HTML>
<BODY>
<h2>Members</h2>
<TABLE>
	<xsl:for-each select="folder/members/member">
	<xsl:sort select="name"/>
	    <tr>
		<TD><xsl:value-of select="nick" /></TD>
	    </tr>
    </xsl:for-each>
</TABLE>
<h2>Files</h2>
<!--Contents of <xsl:value-of select="folder/name"/>/<xsl:value-of select="folder/files/location"/>-->
<xsl:for-each select="folder/files/locationElement">
	<xsl:if test="path != ''"><!-- ignore if root -->
		<A class="path" HREF="#" onclick="showFolder('{/folder/id}', '{path}')"><xsl:value-of select="name" /></A> /
	</xsl:if>	
</xsl:for-each>
<FORM>
<TABLE cellspacing="0" cellpadding="0" class="fileTable" border="0">
	<tr class="fileTableHeader">
	<th></th>
	<th class="spacer"></th>		
	<xsl:choose>
		<xsl:when test="$sortColumn='name' and $sortOrder='ascending'" > 
			<th>
				<A HREF="#" onclick="sortFiles('name')"><xsl:value-of select="folder/files/header/name"/></A>
				<img src="/icons/ascending.gif" alt="ascending" width="8" height="12" border="0" />
			</th>			
		</xsl:when>
		<xsl:when test="$sortColumn='name' and $sortOrder='descending'" >
			<th>
				<A HREF="#" onclick="sortFiles('name')"><xsl:value-of select="folder/files/header/name"/></A>
				<img src="/icons/descending.gif" alt="descending" width="8" height="12" border="0" />
			</th>		
		</xsl:when>
		<xsl:otherwise>
			<th>
				<A HREF="#" onclick="sortFiles('name')"><xsl:value-of select="folder/files/header/name"/></A>		
			</th>		
		</xsl:otherwise>
	</xsl:choose>
	<th class="spacer"></th>
	<xsl:choose>
		<xsl:when test="$sortColumn='size' and $sortOrder='ascending'" >
			<th align="right">
				<A HREF="#" onclick="sortFiles('size')"><xsl:value-of select="folder/files/header/size"/></A>
				<img src="/icons/ascending.gif" alt="ascending" width="8" height="12" border="0" />
			</th>
		</xsl:when>
		<xsl:when test="$sortColumn='size' and $sortOrder='descending'" >
			<th align="right">
				<A HREF="#" onclick="sortFiles('size')"><xsl:value-of select="folder/files/header/size"/></A>					
				<img src="/icons/descending.gif" alt="descending" width="8" height="12" border="0" />
			</th>
		</xsl:when>
		<xsl:otherwise>
			<th>
				<A HREF="#" onclick="sortFiles('size')"><xsl:value-of select="folder/files/header/size"/></A>		
			</th>		
		</xsl:otherwise>
	</xsl:choose>
	<th class="spacer"></th>		
	<xsl:choose>
		<xsl:when test="$sortColumn='modifiedDateMillis' and $sortOrder='ascending'" > 
			<th class="modificationDate">
				<A HREF="#" onclick="sortFiles('modifiedDateMillis')"><xsl:value-of select="folder/files/header/modifiedDate"/></A>
				<img src="/icons/ascending.gif" alt="ascending" width="8" height="12" border="0" />
			</th>			
		</xsl:when>
		<xsl:when test="$sortColumn='modifiedDateMillis' and $sortOrder='descending'" >
			<th class="modificationDate">
				<A HREF="#" onclick="sortFiles('modifiedDateMillis')"><xsl:value-of select="folder/files/header/modifiedDate"/></A>
				<img src="/icons/descending.gif" alt="descending" width="8" height="12" border="0" />
			</th>		
		</xsl:when>
		<xsl:otherwise>
			<th class="modificationDate">
				<A HREF="#" onclick="sortFiles('modifiedDateMillis')"><xsl:value-of select="folder/files/header/modifiedDate"/></A>		
			</th>		
		</xsl:otherwise>
	</xsl:choose>
	</tr>
	
	<xsl:for-each select="folder/files/directory">
	<xsl:sort select="name" order="{$sortOrder}"/>
		<tr>
		<TD><img src="/icons/Directory.gif" alt="F" border="0" width="16" height="16" /></TD>
		<TD></TD>
		<TD><A class="directory" HREF="#" onclick="showFolder('{/folder/id}', '{path}')"><xsl:value-of select="name" /></A></TD>
	   	</tr>		
    </xsl:for-each>
				
	
	<xsl:for-each select="folder/files/file">
		<xsl:sort select="*[name()=$sortColumn]" data-type="{$sortType}" order="{$sortOrder}" />				
	    <tr>		
		<TD><img src="/icon?extension={extension}" alt="F" border="0" width="16" height="16" /></TD>
		<TD></TD>
		
		<xsl:choose>
          	<xsl:when test="isDownloading='true'">
			  	<TD class="isDownloading"><xsl:value-of select="name" /></TD>
			</xsl:when>
			<xsl:when test="isUploading='true'">
			  	<TD class="isUploading"><A HREF="#" onclick="downloadFile('{/folder/id}', '{fullPathAndName}')"><xsl:value-of select="name" /></A></TD>
			</xsl:when>
          	<xsl:when test="isDeleted='true'">
			  	<TD class="isDeleted"><xsl:value-of select="name" /></TD>
			</xsl:when>
          	<xsl:when test="isNewerAvailable='true'">
			  	<TD class="isNewerAvailable"><A HREF="#" onclick="downloadFile('{/folder/id}', '{fullPathAndName}')"><xsl:value-of select="name" /></A></TD>
			</xsl:when>
			<xsl:when test="isExpected='true'">
			  	<TD class="isExpected"><xsl:value-of select="name" /><DIV align="right"><INPUT class="smallbutton" TYPE="button" ID="remotedownloadbutton" name="remotedownload" value="Start Download" onclick="remoteDownloadFile('{/folder/id}', '{fullPathAndName}', {size}, {modifiedDateMillis}, {isDeleted})"/></DIV></TD>
			</xsl:when>
			<xsl:when test="diskFileExists='false'">
			  	<TD class="diskFileNotThere"><xsl:value-of select="name" /></TD>
			</xsl:when>
			
			<xsl:otherwise>
				<TD><A HREF="#" onclick="downloadFile('{/folder/id}', '{fullPathAndName}')"><xsl:value-of select="name" /></A></TD>
			</xsl:otherwise>
        </xsl:choose>				
		<td></td>
		<td class="filesize"><xsl:value-of select="sizeFormatted" /></td>
		<td></td>
		<td class="modificationDate"><xsl:value-of select="modifiedDateFormatted" /></td>
	    </tr>
    </xsl:for-each>
	
</TABLE>
</FORM>	

</BODY>
</HTML>
</xsl:template>

</xsl:stylesheet>