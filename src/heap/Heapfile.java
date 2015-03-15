package heap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

import bufmgr.BufMgrException;
import bufmgr.BufferPoolExceededException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.HashOperationException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageNotReadException;
import bufmgr.PagePinnedException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import diskmgr.DiskMgrException;
import diskmgr.DuplicateEntryException;
import diskmgr.FileEntryNotFoundException;
import diskmgr.FileIOException;
import diskmgr.FileNameTooLongException;
import diskmgr.InvalidPageNumberException;
import diskmgr.InvalidRunSizeException;
import diskmgr.OutOfSpaceException;
import diskmgr.Page;
import global.GlobalConst;
import global.PageId;
import global.RID;
import global.SystemDefs;

public class Heapfile implements GlobalConst {
	private String fileName;
	private Hashtable<Integer, Integer> pagesID;
	private PageId firstPageDirId;

	public Heapfile(String name) throws FileIOException,
			InvalidPageNumberException, DiskMgrException, IOException,
			BufferPoolExceededException, HashOperationException,
			ReplacerException, HashEntryNotFoundException,
			InvalidFrameNumberException, PagePinnedException,
			PageUnpinnedException, PageNotReadException, BufMgrException,
			FileNameTooLongException, InvalidRunSizeException,
			DuplicateEntryException, OutOfSpaceException {
		if (name != null) {
			fileName = name;
			firstPageDirId = null;

			firstPageDirId = SystemDefs.JavabaseDB.get_file_entry(fileName);

			if (firstPageDirId == null) {
				Page page = new Page();

				firstPageDirId = SystemDefs.JavabaseBM.newPage(page, 1);

				// if null Buff manager will throw exception.

				SystemDefs.JavabaseDB.add_file_entry(fileName, firstPageDirId);

				HFPage firstpageDir = new HFPage();

				firstpageDir.init(firstPageDirId, page);
				PageId pgID = new PageId(INVALID_PAGE);

				firstpageDir.setNextPage(pgID);
				firstpageDir.setPrevPage(pgID);
				SystemDefs.JavabaseBM.unpinPage(firstPageDirId, true);
			}
			pagesID = new Hashtable<>();
			initPageIDS();

		}

	}

	private void initPageIDS() throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException, HashEntryNotFoundException {
		PageId currentDirPageId = new PageId(firstPageDirId.pid);

		PageId tempID = new PageId(0);

		HFPage dirPage = new HFPage();

		while (currentDirPageId.pid != INVALID_PAGE) {

			SystemDefs.JavabaseBM.pinPage(currentDirPageId, dirPage, false);
			pagesID.put(currentDirPageId.pid, 0);

			tempID = dirPage.getNextPage();
			SystemDefs.JavabaseBM.unpinPage(currentDirPageId, false);
			currentDirPageId.pid = tempID.pid;
		}
	}

	public RID insertRecord(byte recPtr[]) throws IOException,
			ReplacerException, HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			HashEntryNotFoundException, DiskMgrException,
			SpaceNotAvailableException {
		if (recPtr.length > MAX_SPACE) {
			throw new SpaceNotAvailableException(null, "");
		}

		RID res = null;

		PageId currentDirPageId = new PageId(firstPageDirId.pid);

		PageId tempID = new PageId(0);

		HFPage dirPage = new HFPage();

		boolean foundRec = false;
		while (currentDirPageId.pid != INVALID_PAGE) {

			SystemDefs.JavabaseBM.pinPage(currentDirPageId, dirPage, false);

			if (dirPage.available_space() >= recPtr.length) {

				foundRec = true;
				break;

			}

			tempID = dirPage.getNextPage();
			SystemDefs.JavabaseBM.unpinPage(currentDirPageId, false);
			currentDirPageId.pid = tempID.pid;
		}
		if (foundRec) {
			res = dirPage.insertRecord(recPtr);
			SystemDefs.JavabaseBM.unpinPage(currentDirPageId, true);

		} else {
			HFPage nextDirPage = new HFPage();
			Page pg = new Page();

			tempID = SystemDefs.JavabaseBM.newPage(pg, 1);

			nextDirPage.init(tempID, pg);

			PageId invalid = new PageId(INVALID_PAGE);

			nextDirPage.setNextPage(invalid);
			nextDirPage.setPrevPage(currentDirPageId);

			dirPage.setNextPage(tempID);

			res = nextDirPage.insertRecord(recPtr);
			SystemDefs.JavabaseBM.unpinPage(tempID, true);

		}

		return res;
	}

	public boolean deleteRecord(RID rid) throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException, InvalidSlotNumberException, HashEntryNotFoundException {

		PageId pgID = new PageId(rid.pageNo.pid);
		HFPage dirPage = new HFPage();
		SystemDefs.JavabaseBM.pinPage(pgID, dirPage, false);

		if (pagesID.containsKey(pgID.pid)) {
			dirPage.deleteRecord(rid);
			SystemDefs.JavabaseBM.unpinPage(pgID, true);
			return true;

		}

		SystemDefs.JavabaseBM.unpinPage(pgID, false);

		return false;
	}

	public boolean updateRecord(RID rid, Tuple newtuple)
			throws ReplacerException, HashOperationException,
			PageUnpinnedException, InvalidFrameNumberException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, BufMgrException, InvalidSlotNumberException,
			HashEntryNotFoundException, IOException, InvalidUpdateException {

		PageId pgID = new PageId(rid.pageNo.pid);
		HFPage dirPage = new HFPage();
		SystemDefs.JavabaseBM.pinPage(pgID, dirPage, false);

		if (pagesID.containsKey(pgID.pid)) {
			Tuple oldtuple = new Tuple();
			oldtuple = dirPage.getRecord(rid);

			if (newtuple.getLength() != oldtuple.getLength()) {
				SystemDefs.JavabaseBM.unpinPage(pgID, true);
				throw new InvalidUpdateException(null, "");

			} else {
				SystemDefs.JavabaseBM.unpinPage(pgID, true);
				oldtuple.tupleCopy(newtuple);
				return true;
			}
		}

		SystemDefs.JavabaseBM.unpinPage(pgID, false);

		return false;

	}

	public Tuple getRecord(RID rid) throws ReplacerException,
			PageUnpinnedException, HashEntryNotFoundException,
			InvalidFrameNumberException, HashOperationException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, BufMgrException, IOException,
			InvalidSlotNumberException {

		PageId pgID = new PageId(rid.pageNo.pid);
		HFPage dirPage = new HFPage();
		SystemDefs.JavabaseBM.pinPage(pgID, dirPage, false);

		if (pagesID.containsKey(pgID.pid)) {
			Tuple retTuple = new Tuple();
			retTuple = dirPage.getRecord(rid);

			SystemDefs.JavabaseBM.unpinPage(pgID, true);
			return retTuple;

		}

		return null;

	}

	public int getRecCnt() throws ReplacerException, HashOperationException,
			PageUnpinnedException, InvalidFrameNumberException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, BufMgrException, IOException,
			HashEntryNotFoundException, InvalidSlotNumberException {
		Scan s = openScan();
		RID rid = new RID();
		int recCnt = 0;
		while (s.getNext(rid) != null) {
			recCnt++;
		}

		return recCnt;

	}

	public Scan openScan() throws ReplacerException, HashOperationException,
			PageUnpinnedException, InvalidFrameNumberException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, BufMgrException, IOException {
		Scan scan = new Scan(this);
		return scan;

	}

	public void deleteFile() throws FileEntryNotFoundException,
			FileIOException, InvalidPageNumberException, DiskMgrException,
			IOException {
		SystemDefs.JavabaseDB.delete_file_entry(fileName);
		pagesID = null;
		firstPageDirId = null;
		fileName = null;

	}

	public PageId getfirstDirPageId() {
		return firstPageDirId;
	}

	public Hashtable<Integer, Integer> getPagesID() {
		return pagesID;
	}

}
