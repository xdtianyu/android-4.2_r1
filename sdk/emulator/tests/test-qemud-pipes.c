/* This program is used to test the QEMUD fast pipes.
 * See external/qemu/docs/ANDROID-QEMUD-PIPES.TXT for details.
 *
 * The program acts as a simple TCP server that accepts data and sends
 * them back to the client.
 */

#include <sys/socket.h>
#include <net/inet.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>

#define  DEFAULT_PORT  8012

static void
socket_close(int  sock)
{
    int  old_errno = errno;
    close(sock);
    errno = old_errno;
}

static int
socket_loopback_server( int port, int type )
{
    struct sockaddr_in  addr;

    int  sock = socket(AF_INET, type, 0);
    if (sock < 0) {
        return -1;
    }

    memset(&addr, 0, sizeof(addr));
    addr.sin_family      = AF_INET;
    addr.sin_port        = htons(port);
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    int n = 1;
    setsockopt(s, SOL_SOCKET, SO_REUSEADDR, &n, sizeof(n));

    if (TEMP_FAILURE_RETRY(bind(sock, &addr, sizeof(addr))) < 0) {
        socket_close(sock);
        return -1;
    }

    if (type == SOCK_STREAM) {
        if (TEMP_FAILURE_RETRY(listen(sock, 4)) < 0) {
            socket_close(sock);
            return -1;
        }
    }

    return sock;
}

int main(void)
{
    int sock, client;
    int port = DEFAULT_PORT;

    printf("Starting pipe test server on local port %d\n", port);
    sock = socket_loopback_server( port, SOCK_STREAM );
    if (sock < 0) {
        fprintf(stderr, "Could not start server: %s\n", strerror(errno));
        return 1;
    }

    client = accept(sock, NULL, NULL);
    if (client < 0) {
        fprintf(stderr, "Server error: %s\n", strerror(errno));
        return 2;
    }
    printf("Client connected!\n");

    /* Now, accept any incoming data, and send it back */
    for (;;) {
        char  buff[1024], *p;
        int   ret, count;

        do {
            ret = read(client, buff, sizeof(buff));
        } while (ret < 0 && errno == EINTR);

        if (ret < 0) {
            fprintf(stderr, "Client read error: %s\n", strerror(errno));
            close(client);
            return 3;
        }
        count = ret;
        p     = buff;
        printf("   received: %d bytes\n", count);

        while (count > 0) {
            do {
                ret = write(client, p, count);
            } while (ret < 0 && errno == EINTR);

            if (ret < 0) {
                fprintf(stderr, "Client write error: %s\n", strerror(errno));
                close(client);
                return 4;
            }
            printf("   sent: %d bytes\n", ret);

            p     += ret;
            count -= ret;
        }
    }

    return 0;
}
