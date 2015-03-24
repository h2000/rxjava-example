package rxjava;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;


public class TestRx {

	static Observable<String> fetchWikipediaArticleAsynchronously(final String... wikipediaArticleNames) {
	    return Observable.create( new OnSubscribe<String>(){

			public void call(final Subscriber<? super String> subscriber) {

				new Thread(new Runnable() {

					public void run() {

			            for (final String articleName : wikipediaArticleNames) {
			                if (subscriber.isUnsubscribed()) {
			                    return;
			                }
							InputStream is = null;
							try {
								is = new URL("http://en.wikipedia.org/wiki/" + articleName).openStream();
								final Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
								subscriber.onNext(s.next());
							} catch (final Exception e) {
								subscriber.onError(e);
							} finally {
								try {
									if (is != null) {
										is.close();
									}
								} catch (final IOException e) {
								}
							}

			            }
						if (!subscriber.isUnsubscribed()) {
							subscriber.onCompleted();
			            }

					}
				}).start();
			}
	    });
	}

	public static void main(final String[] args) {

		final String pattern = "a href=\"([^\"]*)\" title=";
		final Pattern regPattern = Pattern.compile(pattern);

		final String x = "<a href=\"/wiki/Tigress_(disambiguation)\" title=\"Tigress (disambiguation)\" class=\"mw-disambig\">Tigress (disambiguation)</a>.</div>";
		final Matcher matcher = regPattern.matcher(x);
		while (matcher.find()) {
			System.out.println(matcher.group(1));
		}
		final Observable<String> fetchFromTopic = fetchWikipediaArticleAsynchronously("Tiger", "Elephant", "Alf", "Bush", "x");

		final Func1<String, Observable<String>> extractLinksFromHtmlString = new Func1<String, Observable<String>>() {

			public Observable<String> call(final String htmlString) {

				return Observable.create(new OnSubscribe<String>() {

					public void call(final Subscriber<? super String> subscriber) {

						subscriber.onStart();
						while (matcher.find()) {
							final String link = matcher.group(1);
							if (link.startsWith("http")) {
								continue;
							}
							if (link.startsWith("//")) {
								continue;
							}

							subscriber.onNext(link);
						}
						subscriber.onCompleted();
					}
				});
			}
		};
		final Observable<String> linksOfToppig = fetchFromTopic.flatMap(extractLinksFromHtmlString);

		fetchFromTopic.subscribe(new Action1<String>() {

			public void call(final String s) {

				final Matcher matcher = regPattern.matcher(s);
				System.out.println("\n------------------------\nreaded:" + s.substring(0, 150));
				while (matcher.find()) {
					System.out.println("->" + matcher.group(1));
				}
			}
		}, new Action1<Throwable>() {

			public void call(final Throwable t1) {

				System.err.println(t1);
			}
		}, new Action0() {

			public void call() {

				System.out.println("\n------------------------\nfinished");

			}
		});

	}

}
