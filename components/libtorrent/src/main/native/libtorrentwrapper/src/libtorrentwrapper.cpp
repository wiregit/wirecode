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

//TODO fix memory leaks

extern "C" int init(const char* path) {
	std::string newPath(path);
	savePath = newPath;
	s.set_alert_mask(0xffffffff);
	s.listen_on(std::make_pair(6881, 6889));
	return 1;
}

extern "C" int abort_torrents() {
	s.abort();
	return 1;
}

std::string* getSizeTypeString(libtorrent::size_type size) {
	std::stringstream oss;
	oss << size;
	std::string* sizeString = new std::string(oss.str().c_str());
	//TODO clean memory
	return sizeString;
}

std::string* getSha1String(sha1_hash sha1) {
	std::stringstream oss;
	oss << sha1;

	std::string* sha1String = new std::string(oss.str().c_str());
	//TODO clean memory
	return sha1String;
}

sha1_hash getSha1Hash(const char* sha1String) {
	sha1_hash sha1;
	std::stringstream oss;
	oss << sha1String;
	oss >> sha1;
	return sha1;
}

struct torrent_s {
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

void get_torrent_s(libtorrent::torrent_handle handle, torrent_s* stats) {
	libtorrent::torrent_status status = handle.status();

	float download_rate = status.download_rate;
	float upload_rate = status.upload_rate;
	//TODO cleanup memory
	const char* total_done = getSizeTypeString(status.total_done)->c_str();
	const char* total_download =
			getSizeTypeString(status.total_download)->c_str();
	const char* total_upload = getSizeTypeString(status.total_upload)->c_str();
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

struct info_s {
	const char* sha1;
	const char* name;
	int piece_length;
	int num_pieces;
	int num_files;
	const char* content_length;
	const char** paths;
};

extern "C" int move_torrent(const char* id, const char* path) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		std::string newPath(path);
		h.move_storage(newPath);
		return 1;
	} else {
		return 0;
	}
}

extern "C" const void* add_torrent_existing(char* sha1String, char* trackerURI, char* fastResumePath) {

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
	
	
	boost::filesystem::ifstream resume_file(fastResumePath, std::ios_base::binary);
	resume_file.unsetf(std::ios_base::skipws);
	
	std::istream_iterator<char> ios_iter;
	std::istream_iterator<char> iter(resume_file);
	
	std::copy(iter, ios_iter,std::back_inserter(resume_buf));
	
	p.resume_data = &resume_buf;

	libtorrent::torrent_handle h = s.add_torrent(p);
	h.resume();
}

extern "C" const void* add_torrent(char* path) {
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

	libtorrent::torrent_info torrent_info = h.get_torrent_info();

	const char* name = torrent_info.name().c_str();
	int piece_length = torrent_info.piece_length();
	int num_pieces = torrent_info.num_pieces();
	int num_files = torrent_info.num_files();

	//TODO cleanup memory
	std::string* content_length = getSizeTypeString(torrent_info.total_size());

	#ifdef LIMEDEBUG
	std::cout << "total_size_unknown: " << torrent_info.total_size()
			<< std::endl;
	std::cout << "total_size_long: " << content_length << std::endl;
	#endif

	libtorrent::file_storage files = torrent_info.files();

	const char** paths = new const char*[num_files];
	for (int i = 0; i < num_files; i++) {
		libtorrent::file_entry file = files.at(i);
		boost::filesystem::path path = file.path;
		const char* p = path.string().c_str();
		paths[i] = p;
	}

	sha1_hash sha1 = torrent_info.info_hash();
	#ifdef LIMEDEBUG
	std::cout << "sha1: " << sha1 << std::endl;
	#endif

	const char* sha1String = getSha1String(sha1)->c_str();

	#ifdef LIMEDEBUG
	std::cout << "sha1String: " << sha1String << std::endl;
	#endif

	//TODO free memory
	info_s* info = new info_s();
	info->sha1 = sha1String;
	info->name = name;
	info->num_files = num_files;
	info->num_pieces = num_pieces;
	info->piece_length = piece_length;
	info->content_length = content_length->c_str();
	info->paths = paths;

	return info;
}

extern "C" int pause_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		h.pause();
		return 1;
	} else {
		return 0;
	}

}

extern "C" int remove_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		s.remove_torrent(h);
		return 1;
	} else {
		return 0;
	}
}

extern "C" int resume_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		h.resume();
		return 1;
	} else {
		return 0;
	}
}

extern "C" void* get_torrent_status(const char* id, void* stat) {
	struct torrent_s* stats = (struct torrent_s *) stat;

	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		get_torrent_s(h, stats);
	} else {
		stats->valid = false;
	}
	return stats;
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

struct alert_s {
	int category;
	const char* sha1;
	const char* message;
	const char* data;

	alert_s() {
		sha1 = 0;
		category = 0;
		message = 0;
		data = 0;
	}
};

extern "C" int get_num_peers(const char* id) {

	libtorrent::torrent_handle h = findTorrentHandle(id);

	if (!h.is_valid())  return 0;

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

	if (!h.is_valid())  return;

	std::vector<libtorrent::peer_info> *peers = new std::vector<libtorrent::peer_info>();
	h.get_peer_info(*peers);

	int pos = 0;

	std::vector<libtorrent::peer_info>::iterator iter = peers->begin();

	while( iter != peers->end() ) {

		std::string address = iter->ip.address().to_string();
		int len = address.length();

		#ifdef LIMEDEBUG
		std::cout << address << std::endl ;
		#endif

		if (len+pos > buffer_len)  break;

		for ( int i=0 ; i<len ; i++ )
			data[pos+i] = address[i];

		pos += len+1;
		data[pos++] = ';';
		++iter;
	}

	delete peers;
}

void process_save_resume_data_alert(libtorrent::torrent_handle handle, 
				     libtorrent::save_resume_data_alert const* alert, 
				     alert_s* alertStatus) 
{	#ifdef LIMEDEBUG
	std::cout << "save_resume_data_alert" << std::endl;
	#endif
	
	std::string resume_data_file = handle.get_torrent_info().name() + ".fastresume";
	
	boost::filesystem::path path(handle.save_path() / resume_data_file);
	
	boost::filesystem::ofstream out(path, std::ios_base::binary);
        out.unsetf(std::ios_base::skipws);
        libtorrent::bencode(std::ostream_iterator<char>(out), *alert->resume_data);
			
	alertStatus->data = path.file_string().c_str();
}

void process_alert(libtorrent::alert* alert, alert_s* alertStatus) {

	alertStatus->category = alert->category();
	alertStatus->message = alert->message().c_str();

	libtorrent::torrent_alert* torrentAlert;

	if (torrentAlert = dynamic_cast<libtorrent::torrent_alert*>(alert)) {

		libtorrent::torrent_handle handle = torrentAlert->handle;

		if (handle.is_valid()) {
			const char* sha1 = getSha1String(handle.info_hash())->c_str();
			alertStatus->sha1 = sha1;

			libtorrent::save_resume_data_alert const* rd = dynamic_cast<libtorrent::save_resume_data_alert const*>(alert);

			if (rd) {
				process_save_resume_data_alert(handle, rd, alertStatus);
				return;
			}

			libtorrent::save_resume_data_failed_alert const* rd2 = dynamic_cast<libtorrent::save_resume_data_failed_alert const*>(alert);

			if (rd2) {
				#ifdef LIMEDEBUG
				std::cout << "save_resume_data_failed_alert (" << rd2->msg << ")" << std::endl;
				#endif
				alertStatus->message = rd2->msg.c_str();
				return;
			}

			libtorrent::fastresume_rejected_alert const* rd3 = dynamic_cast<libtorrent::fastresume_rejected_alert const*>(alert);

			if (rd3) {
				#ifdef LIMEDEBUG
				std::cout << "fastresume_rejected_alert (" << rd3->msg << ")" << std::endl;
				#endif
				alertStatus->message = rd3->msg.c_str();
				return;
			}

		}
	}

}

extern "C" void get_alerts(void(*alertCallback)(void*)) {

	std::auto_ptr<libtorrent::alert> alerts;

	alerts = s.pop_alert();

	while (alerts.get()) {
		libtorrent::alert* alert = alerts.get();

		alert_s* alertStatus = new alert_s();

		process_alert(alert, alertStatus);

		alertCallback(alertStatus);

		delete alertStatus;

		alerts = s.pop_alert();
	}
}

extern "C" void print() {
	std::cout << "print called!" << std::endl;
}

