package de.dal33t.powerfolder.util.delta;

public class MatchInfo {
	private PartInfo matchedPart;
	private long matchedPosition;
	public MatchInfo(PartInfo matchedPart, long matchedPosition) {
		super();
		this.matchedPart = matchedPart;
		this.matchedPosition = matchedPosition;
	}
	public PartInfo getMatchedPart() {
		return matchedPart;
	}
	public long getMatchedPosition() {
		return matchedPosition;
	}
}
