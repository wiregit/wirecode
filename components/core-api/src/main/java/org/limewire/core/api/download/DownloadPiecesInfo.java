package org.limewire.core.api.download;

public interface DownloadPiecesInfo {

  public enum PieceState {
      DOWNLOADED, PARTIAL, AVAILABLE, ACTIVE, UNAVAILABLE;
  }
  
  public PieceState getPieceState(int piece);
  public int getNumPieces();
}
