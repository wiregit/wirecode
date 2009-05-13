#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <memory>

#include "libtorrent/config.hpp"
#include "libtorrent/session.hpp"
#include "libtorrent/peer_info.hpp"
#include  "boost/shared_ptr.hpp"
#include "libtorrent/alert.hpp"
#include "libtorrent/alert_types.hpp"
#include "libtorrent/peer_id.hpp"
#include <boost/filesystem/path.hpp>
#include "libtorrent/size_type.hpp"

#include "libtorrent/entry.hpp"
#include "libtorrent/bencode.hpp"
#include "libtorrent/session.hpp"
#include "libtorrent/identify_client.hpp"
#include "libtorrent/alert_types.hpp"
#include "libtorrent/ip_filter.hpp"
#include "libtorrent/magnet_uri.hpp"
#include "libtorrent/bitfield.hpp"
#include "libtorrent/file.hpp"

#include "libtorrent/socket.hpp"
using libtorrent::asio::ip::tcp;

#ifdef WINDOWS
#include <windows.h>
#endif

libtorrent::session s;
std::string savePath;
typedef libtorrent::big_number sha1_hash;

#ifdef NO_ERROR
#define RET int
#define wTHROW(x) if (x) return 1; else return 0;
#else
#define RET wrapper_status*
#define wTHROW(x) if (last_error) { delete last_error; last_error=0;} last_error = x; return last_error;
#endif

struct wrapper_status {
	int type;
	const char* message;

	wrapper_status(int type, const char* message) {
		this->type = type;
		this->message = message;
	}

	wrapper_status() {
		this->type = -1;
		this->message = "unfilled";
	}
};
wrapper_status* last_error = 0;

struct wrapper_torrent_status {
	const char* total_done;
	const char* total_download;
	const char* total_upload;
	float download_rate;
	float upload_rate;
	int num_peers;
	int num_uploads;
	int num_seeds;
	int num_connections;
	int state;
	float progress;
	int paused;
	int finished;
	int valid;
};

struct wrapper_alert_info {
	int category;
	char* sha1;
	const char* message;
	const char* data;

	wrapper_alert_info() {
		sha1 = new char[41];
		sha1[0] = 0;
		category = 0;
		message = 0;
		data = 0;
	}

	~wrapper_alert_info() {
		delete sha1;
	}
};

void getSizeTypeString(libtorrent::size_type size, char* heap) {
	std::stringstream oss;
	oss << size;
	std::string str = oss.str();
	const char* chars = str.c_str();

	//memcpy(&heap, &chars, str.length()+1);
	for (int i = 0; i < str.length(); i++) {
		heap[i] = chars[i];
	}
	heap[str.length()] = '\0';
}

void getSha1String(sha1_hash sha1, char* heap) {
	std::stringstream oss;
	oss << sha1;
	std::string str = oss.str();
	const char* chars = str.c_str();

	//memcpy(&heap, &chars, str.length()+1);
	for (int i = 0; i < str.length(); i++) {
		heap[i] = chars[i];
	}
	heap[str.length()] = '\0';
}

sha1_hash getSha1Hash(const char* sha1String) {
	sha1_hash sha1;
	std::stringstream oss;
	oss << sha1String;
	oss >> sha1;
	return sha1;
}

void get_wrapper_torrent_status(libtorrent::torrent_handle handle,
		wrapper_torrent_status* stats) {
	libtorrent::torrent_status status = handle.status();

	char* total_done = new char[20];
	char* total_download = new char[20];
	char* total_upload = new char[20];

	float download_rate = status.download_rate;
	float upload_rate = status.upload_rate;

	getSizeTypeString(status.total_done, total_done);
	getSizeTypeString(status.total_download, total_download);
	getSizeTypeString(status.total_upload, total_upload);

	int num_peers = status.num_peers;
	int num_uploads = status.num_uploads;
	int num_seeds = status.num_seeds;
	int num_connections = status.num_connections;
	int state = status.state;
	float progress = status.progress;
	bool paused = status.paused;
	bool finished = handle.is_finished();
	bool valid = handle.is_valid();

	stats->total_done = total_done;
	stats->total_download = total_download;
	stats->total_upload = total_upload;
	stats->download_rate = download_rate;
	stats->upload_rate = upload_rate;
	stats->num_peers = num_peers;
	stats->num_uploads = num_uploads;
	stats->num_seeds = num_seeds;
	stats->num_connections = num_connections;
	stats->state = state;
	stats->progress = progress;
	stats->paused = paused;
	stats->finished = finished;
	stats->valid = valid;

}

libtorrent::torrent_handle findTorrentHandle(const char* sha1String) {
	sha1_hash sha1 = getSha1Hash(sha1String);
	libtorrent::torrent_handle torrent_handle = s.find_torrent(sha1);
	return torrent_handle;
}

void process_save_resume_data_alert(libtorrent::torrent_handle handle,
		libtorrent::save_resume_data_alert const* alert,
		wrapper_alert_info* alertInfo) {
#ifdef LIMEDEBUG_RESUME
	std::cout << "save_resume_data_alert";
#endif

	std::string resume_data_file = handle.get_torrent_info().name()
			+ ".fastresume";
	boost::filesystem::path path(handle.save_path() / resume_data_file);
	alertInfo->data = path.file_string().c_str();

#ifdef LIMEDEBUG_RESUME
	std::cout << "(to " << alertStatus->data << ')' << std::endl;
#endif

	boost::filesystem::ofstream out(path, std::ios_base::binary);
	out.unsetf(std::ios_base::skipws);
	libtorrent::bencode(std::ostream_iterator<char>(out), *alert->resume_data);
}

void process_alert(libtorrent::alert* alert, wrapper_alert_info* alertInfo) {

	alertInfo->category = alert->category();
	alertInfo->message = alert->message().c_str();

	libtorrent::torrent_alert* torrentAlert;

	if (torrentAlert = dynamic_cast<libtorrent::torrent_alert*> (alert)) {

		libtorrent::torrent_handle handle = torrentAlert->handle;

		if (handle.is_valid()) {

			getSha1String(handle.info_hash(), alertInfo->sha1);

			libtorrent::save_resume_data_alert const
					* srd_alert =
							dynamic_cast<libtorrent::save_resume_data_alert const*> (alert);
			if (srd_alert) {
				process_save_resume_data_alert(handle, srd_alert, alertInfo);
				return;
			}

			libtorrent::save_resume_data_failed_alert const
					* srdf_alert =
							dynamic_cast<libtorrent::save_resume_data_failed_alert const*> (alert);
			if (srdf_alert) {
#ifdef LIMEDEBUG_RESUME
				std::cout << "save_resume_data_failed_alert (" << srdf_alert->msg << ")" << std::endl;
#endif
				alertInfo->message = srdf_alert->msg.c_str();
				return;
			}

			libtorrent::fastresume_rejected_alert const
					* fra_alert =
							dynamic_cast<libtorrent::fastresume_rejected_alert const*> (alert);
			if (fra_alert) {
#ifdef LIMEDEBUG_RESUME
				std::cout << "fastresume_rejected_alert (" << fra_alert->msg << ")" << std::endl;
#endif
				alertInfo->message = fra_alert->msg.c_str();
				return;
			}
		}
	}
}

extern "C" RET init(const char* path) {
	std::string newPath(path);
	savePath = newPath;
	s.set_alert_mask(0xffffffff);
	s.listen_on(std::make_pair(6881, 6889));

	return 0;
}

extern "C" RET abort_torrents() {
	s.abort();
	return 0;
}

extern "C" RET move_torrent(const char* id, const char* path) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		std::string newPath(path);
		h.move_storage(newPath);
		return 0;
	}

	wTHROW(new wrapper_status());
}

extern "C" RET add_torrent_existing(char* sha1String, char* trackerURI,
		char* fastResumePath) {

#ifdef LIMEDEBUG
	std::cout << "adding torrent" << std::endl;
	std::cout << "sha1String" << sha1String << std::endl;
	std::cout << "trackerURI" << trackerURI << std::endl;
#endif
	sha1_hash sha1 = getSha1Hash(sha1String);
#ifdef LIMEDEBUG
	std::cout << "sha1_hash" << sha1 << std::endl;
#endif

	libtorrent::add_torrent_params p;
	p.save_path = savePath;
	p.info_hash = sha1;
	p.tracker_url = trackerURI;
	p.auto_managed = false;

	std::vector<char> resume_buf;

	if (fastResumePath) {
		boost::filesystem::ifstream resume_file(fastResumePath,
				std::ios_base::binary);

		if (!resume_file.fail()) {
			resume_file.unsetf(std::ios_base::skipws);

			std::istream_iterator<char> ios_iter;
			std::istream_iterator<char> iter(resume_file);

			std::copy(iter, ios_iter, std::back_inserter(resume_buf));

			p.resume_data = &resume_buf;
		}
	}

	libtorrent::torrent_handle h = s.add_torrent(p);
	h.resume();

	return 0;
}

extern "C" RET add_torrent(char* path) {
#ifdef LIMEDEBUG
	std::cout << "adding torrent" << std::endl;
	std::cout << "path: " << path << std::endl;
#endif
	libtorrent::add_torrent_params p;
	p.save_path = savePath;
	p.ti = new libtorrent::torrent_info(path);
	p.auto_managed = false;
	libtorrent::torrent_handle h = s.add_torrent(p);
	h.resume();

	// TODO: ?
	// delete p.ti;
	return 0;
}

extern "C" RET pause_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		h.pause();
		return 0;
	}

	wTHROW(new wrapper_status());
}

extern "C" RET remove_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		s.remove_torrent(h);
		return 0;
	}

	wTHROW(new wrapper_status());
}

extern "C" RET resume_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		h.resume();
		return 0;
	}

	wTHROW(new wrapper_status());
}

extern "C" RET get_torrent_status(const char* id, void* stat) {

	struct wrapper_torrent_status* stats =
			(struct wrapper_torrent_status *) stat;

	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		get_wrapper_torrent_status(h, stats);
	} else {
		stats->valid = false;
	}

	return 0;
}

extern "C" bool signal_fast_resume_data_request(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);

	if (h.is_valid() && h.has_metadata()) {
		h.save_resume_data();
		return 1;
	} else {
		return 0;
	}
}

extern "C" int get_num_peers(const char* id) {

	libtorrent::torrent_handle h = findTorrentHandle(id);

	if (!h.is_valid()) {
		return 0;
	}

	libtorrent::torrent_status s = h.status();

	if (s.state == libtorrent::torrent_status::seeding)
		return 0;

	std::vector<libtorrent::peer_info> peers;

	try {
		// TODO: This is failing?  Internal libtorrent error?
		h.get_peer_info(peers);
	} catch (libtorrent::invalid_handle e) {
		return 0;
	} catch (std::exception e) {
		return 0;
	}

	int size = peers.size();

	return size;
}

extern "C" void get_peers(const char* id, int buffer_len, char* data) {

	libtorrent::torrent_handle h = findTorrentHandle(id);

	if (!h.is_valid())
		return;

	std::vector<libtorrent::peer_info> *peers = new std::vector<
			libtorrent::peer_info>();
	h.get_peer_info(*peers);

	int pos = 0;

	std::vector<libtorrent::peer_info>::iterator iter = peers->begin();

	while (iter != peers->end()) {

		std::string address = iter->ip.address().to_string();
		int len = address.length();

#ifdef LIMEDEBUG
		std::cout << "peer:" << address << std::endl;
#endif

		if (len + pos > buffer_len)
			break;

		for (int i = 0; i < len; i++) {
			data[pos + i] = address[i];
		}

		pos += len + 1;
		data[pos++] = ';';
		++iter;
	}

	delete peers;
}

extern "C" RET get_alerts(void(*alertCallback)(void*)) {

	std::auto_ptr<libtorrent::alert> alerts;

	alerts = s.pop_alert();

	while (alerts.get()) {
		libtorrent::alert* alert = alerts.get();

		wrapper_alert_info* alertInfo = new wrapper_alert_info();

		process_alert(alert, alertInfo);
		alertCallback(alertInfo);

		delete alertInfo;

		alerts = s.pop_alert();
	}

	return 0;
}

extern "C" void free_torrent_status(wrapper_torrent_status* info) {
	delete[] info->total_done;
	delete[] info->total_download;
	delete[] info->total_upload;
}
