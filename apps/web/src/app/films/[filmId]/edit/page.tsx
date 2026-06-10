import { CreateFilmClient } from "@/components/CreateFilmClient";

type Props = {
  params: Promise<{
    filmId: string;
  }>;
};

export default async function EditFilmPage({ params }: Props) {
  const { filmId } = await params;
  return <CreateFilmClient editFilmId={Number(filmId)} />;
}
