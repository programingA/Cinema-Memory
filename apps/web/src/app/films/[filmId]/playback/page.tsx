import { PlaybackClient } from "@/components/PlaybackClient";

type Props = {
  params: Promise<{
    filmId: string;
  }>;
};

export default async function PlaybackPage({ params }: Props) {
  const { filmId } = await params;
  return <PlaybackClient filmId={Number(filmId)} />;
}
