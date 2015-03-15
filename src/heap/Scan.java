package heap;

import java.io.IOException;

import bufmgr.BufMgrException;
import bufmgr.BufferPoolExceededException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.HashOperationException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageNotReadException;
import bufmgr.PagePinnedException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import global.PageId;
import global.RID;
import global.SystemDefs;

public class Scan {
	private PageId firstDirPageID;
	
	private RID cursorRID;
	private Heapfile heapFile;


	public Scan(Heapfile hf) throws ReplacerException, HashOperationException,
			PageUnpinnedException, InvalidFrameNumberException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, BufMgrException, IOException {
		
		heapFile = hf;

		firstDirPageID = hf.getfirstDirPageId();

		HFPage firstPage = new HFPage();
		
		SystemDefs.JavabaseBM.pinPage(firstDirPageID, firstPage, false);
		cursorRID = new RID(firstDirPageID, 0);

	}

	public Tuple getNext(RID rid) throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException, InvalidSlotNumberException, HashEntryNotFoundException {


		if (cursorRID.pageNo.pid != -1) {
			rid.copyRid(cursorRID);

			HFPage cursorPage = new HFPage();

			SystemDefs.JavabaseBM.pinPage(cursorRID.pageNo, cursorPage, false);
			cursorPage.getCurPage().pid = rid.pageNo.pid;
			if (cursorPage.getSlotLength(cursorRID.slotNo) == -1) {
				SystemDefs.JavabaseBM.unpinPage(firstDirPageID, false);
				SystemDefs.JavabaseBM.unpinPage(cursorRID.pageNo, false);
				return null;
			}
			Tuple retTuple = cursorPage.getRecord(cursorRID);

//			if (cursorPage.nextRecord(cursorRID) == null) {
//				PageId temp = cursorPage.getNextPage();
//				SystemDefs.JavabaseBM.unpinPage(cursorRID.pageNo, false);
//				cursorRID.pageNo = temp;
//				cursorRID = new RID(cursorRID.pageNo,
//						cursorPage.firstRecord().slotNo);
//
//			} else {
				
				cursorRID = new RID(cursorRID.pageNo, cursorRID.slotNo + 1);
				SystemDefs.JavabaseBM.unpinPage(cursorRID.pageNo, false);
//			}

			return retTuple;

		}
		SystemDefs.JavabaseBM.unpinPage(firstDirPageID, false);
		return null;
	}

	public boolean position(RID rid) throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException, HashEntryNotFoundException, InvalidSlotNumberException {
		if (heapFile.getPagesID().contains(rid.pageNo.pid)) {
			PageId dirPageId = new PageId(rid.pageNo.pid);

			HFPage dirPage = new HFPage();

			SystemDefs.JavabaseBM.pinPage(dirPageId, dirPage, false);
			if (dirPage.getRecord(rid) != null) {
				cursorRID.pageNo.pid = rid.pageNo.pid;
				cursorRID.copyRid(rid);
				return true;
			}

		}

		return false;
	}

	public void closescan() {

		cursorRID.pageNo = null;
		cursorRID = null;
	}

}
